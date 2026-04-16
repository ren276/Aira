package com.aira.health.util.permission

import android.util.Log
import android.content.Context
import com.aira.health.BuildConfig
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Health Connect permission batches.
 * Batch 1 (Core): HR, HRV, Sleep, Steps, Activity — required to compute core scores
 * Batch 2 (Body): Weight, SpO2, Nutrition, Hydration, Skin Temp, BMR, Body Fat
 * Batch 3 (Advanced): Blood glucose, Blood pressure, Cycle tracking, Respiratory rate
 *
 * UX: Hard block on Core denial (loop with "Use limited mode" escape).
 * Re-prompt on every cold launch.
 */
@Singleton
class HealthPermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "AiraHealthPerms"
    }

    enum class PermissionBatch { CORE, BODY, ADVANCED }

    val coreBatchPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
    )

    val bodyBatchPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(LeanBodyMassRecord::class),
        HealthPermission.getReadPermission(BasalMetabolicRateRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getReadPermission(SkinTemperatureRecord::class),
    )

    val advancedBatchPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(BloodGlucoseRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class),
        HealthPermission.getReadPermission(Vo2MaxRecord::class),
        HealthPermission.getReadPermission(MenstruationFlowRecord::class),
        HealthPermission.getReadPermission(OvulationTestRecord::class),
        HealthPermission.getReadPermission(CervicalMucusRecord::class),
    )

    val writePermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getWritePermission(NutritionRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
    )

    fun getPermissionsForBatch(batch: PermissionBatch): Set<String> = when (batch) {
        PermissionBatch.CORE -> coreBatchPermissions
        PermissionBatch.BODY -> bodyBatchPermissions
        PermissionBatch.ADVANCED -> advancedBatchPermissions
    }

    fun isBatchSatisfied(
        grantedPermissions: Set<String>,
        batch: PermissionBatch
    ): Boolean {
        val batchPermissions = getPermissionsForBatch(batch)
        return batchPermissions.any { it in grantedPermissions }
    }

    /**
     * Check Health Connect availability on this device.
     * Android 10-13: Health Connect app must be installed from Play Store.
     * Android 14+: Health Connect is built-in.
     */
    fun getHealthConnectStatus(): HealthConnectStatus {
        val sdkStatus = HealthConnectClient.getSdkStatus(context)
        val status = when (sdkStatus) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectStatus.Available
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectStatus.UpdateRequired
            else -> HealthConnectStatus.NotInstalled
        }
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Health Connect sdkStatus=$sdkStatus resolvedStatus=${status::class.simpleName}")
        }
        return status
    }

    suspend fun getGrantedPermissions(): Set<String> {
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        if (BuildConfig.DEBUG) {
            Log.i(
                TAG,
                "Granted permissions -> count=${granted.size} values=[${granted.sorted().joinToString()}]"
            )
        }
        return granted
    }

    suspend fun isCoreGranted(): Boolean {
        val granted = getGrantedPermissions()
        val result = isBatchSatisfied(granted, PermissionBatch.CORE)
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Core batch granted=$result grantedCount=${granted.size} batchSize=${coreBatchPermissions.size}")
        }
        return result
    }

    suspend fun isBodyGranted(): Boolean {
        val granted = getGrantedPermissions()
        val result = isBatchSatisfied(granted, PermissionBatch.BODY)
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Body batch granted=$result grantedCount=${granted.size} batchSize=${bodyBatchPermissions.size}")
        }
        return result
    }

    suspend fun isAdvancedGranted(): Boolean {
        val granted = getGrantedPermissions()
        val result = isBatchSatisfied(granted, PermissionBatch.ADVANCED)
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Advanced batch granted=$result grantedCount=${granted.size} batchSize=${advancedBatchPermissions.size}")
        }
        return result
    }
}

sealed class HealthConnectStatus {
    object Available : HealthConnectStatus()
    object UpdateRequired : HealthConnectStatus()
    object NotInstalled : HealthConnectStatus()
}
