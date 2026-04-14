package com.aira.health.util.permission

import android.content.Context
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
        HealthPermission.getReadPermission(BloodOxygenSaturationRecord::class),
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

    /**
     * Check Health Connect availability on this device.
     * Android 10-13: Health Connect app must be installed from Play Store.
     * Android 14+: Health Connect is built-in.
     */
    fun getHealthConnectStatus(): HealthConnectStatus {
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectStatus.Available
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectStatus.UpdateRequired
            else -> HealthConnectStatus.NotInstalled
        }
    }

    suspend fun getGrantedPermissions(): Set<String> {
        val client = HealthConnectClient.getOrCreate(context)
        return client.permissionController.getGrantedPermissions()
    }

    suspend fun isCoreGranted(): Boolean {
        val granted = getGrantedPermissions()
        return coreBatchPermissions.all { it in granted }
    }

    suspend fun isBodyGranted(): Boolean {
        val granted = getGrantedPermissions()
        return bodyBatchPermissions.all { it in granted }
    }
}

sealed class HealthConnectStatus {
    object Available : HealthConnectStatus()
    object UpdateRequired : HealthConnectStatus()
    object NotInstalled : HealthConnectStatus()
}
