package com.aira.health.presentation.dashboard.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.dao.HrSampleDao
import com.aira.health.data.local.dao.HrvSampleDao
import com.aira.health.data.local.dao.SleepSessionDao
import com.aira.health.data.local.dao.WorkoutSessionDao
import com.aira.health.data.local.model.DailyMetrics
import com.aira.health.data.local.model.HrSample
import com.aira.health.data.local.model.SleepSession
import com.aira.health.data.local.model.WorkoutSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Shared ViewModel handling the data contract for all metric detail screens (D-05).
 * Resolves the metric ID from navigation arguments, loads trailing trend data,
 * and maintains the D-11 explanation state.
 */
@HiltViewModel
class MetricDetailViewModel @Inject constructor(
    private val dailyMetricsDao: DailyMetricsDao,
    private val hrSampleDao: HrSampleDao,
    private val hrvSampleDao: HrvSampleDao,
    private val sleepSessionDao: SleepSessionDao,
    private val workoutSessionDao: WorkoutSessionDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Argument parsing is aligned with MetricDetailRoute contract
    private val metricId: String = savedStateHandle["metricId"] ?: MetricType.RECOVERY.id
    
    // T-04-07: Mitigate invalid route arguments by fallback/erroring securely
    val metricType: MetricType? = MetricType.fromIdOrNull(metricId)

    /** Trend data for the metric, spanning the last 14 days */
    private val trendFlow = flow {
        // Fetch trailing 14 days
        val recent = dailyMetricsDao.getLast14Days()
        // Ensure they are ordered chronologically (oldest first)
        val sorted = recent.sortedBy { it.date }
        emit(sorted)
    }

    /** Today's value */
    private val todayFlow = flow {
        val todayStr = LocalDate.now().toString()
        val metrics = dailyMetricsDao.getRange(todayStr, todayStr).firstOrNull()
        emit(metrics)
    }

    val uiState: StateFlow<MetricDetailUiState> = combine(
        trendFlow,
        todayFlow
    ) { trendMetrics, todayMetrics ->
        if (metricType == null) {
            return@combine MetricDetailUiState.Error("Invalid or unknown metric type: $metricId")
        }

        if (todayMetrics == null && trendMetrics.isEmpty()) {
            return@combine MetricDetailUiState.Error("No data available for $metricId")
        }

        val activeMetrics = todayMetrics ?: trendMetrics.last()
        val currentScore = extractScore(activeMetrics, metricType)

        // Map trend data into continuous floats
        val dataPoints = trendMetrics.map { extractScore(it, metricType).toFloat() }

        val previousScore = trendMetrics
            .dropLast(1)
            .lastOrNull()
            ?.let { extractScore(it, metricType) }
        val trendAverage = if (dataPoints.isNotEmpty()) dataPoints.average().toFloat() else currentScore.toFloat()
        val delta = previousScore?.let { currentScore - it } ?: 0
        val vsAverage = currentScore - trendAverage
        val evidence = buildMetricEvidence(metricType, activeMetrics, trendMetrics)

        MetricDetailUiState.Success(
            metricType = metricType,
            currentScore = currentScore,
            trendDataPoints = dataPoints,
            confidence = activeMetrics.dataConfidence, // T-04-08 preserved provenance
            whatChanged = buildWhatChanged(metricType, currentScore, delta, vsAverage),
            whyItMatters = buildWhyItMatters(metricType, activeMetrics.dataConfidence),
            whatToDoNext = buildWhatToDoNext(metricType, currentScore),
            dataSources = evidence.sources,
            consideredData = evidence.dataPoints
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MetricDetailUiState.Loading
    )

    private fun extractScore(metrics: DailyMetrics, type: MetricType): Int {
        return when (type) {
            MetricType.RECOVERY -> metrics.recoveryScore
            MetricType.SLEEP -> metrics.sleepScore
            MetricType.STRAIN -> metrics.strainScore
            MetricType.STRESS -> metrics.stressScore
        }
    }

    private fun buildWhatChanged(
        type: MetricType,
        current: Int,
        deltaVsYesterday: Int,
        deltaVsAverage: Float
    ): String = when (type) {
        MetricType.RECOVERY ->
            "Recovery is $current (${signed(deltaVsYesterday)} vs yesterday, ${signed(deltaVsAverage.toInt())} vs 14-day trend)."
        MetricType.SLEEP ->
            "Sleep score is $current (${signed(deltaVsYesterday)} vs yesterday) with a ${signed(deltaVsAverage.toInt())} shift against your recent baseline."
        MetricType.STRAIN ->
            "Strain is $current (${signed(deltaVsYesterday)} vs yesterday), indicating ${if (current >= 70) "elevated" else "managed"} training load."
        MetricType.STRESS ->
            "Stress is $current (${signed(deltaVsYesterday)} vs yesterday), currently ${if (current >= 65) "above" else "within"} your normal daily band."
    }

    private fun buildWhyItMatters(type: MetricType, confidence: Float): String {
        val confidencePct = (confidence * 100).toInt().coerceIn(0, 100)
        val confidenceClause = "Model confidence is $confidencePct%, so this guidance reflects your current local data quality."

        return when (type) {
            MetricType.RECOVERY ->
                "Recovery summarizes how ready your system is for adaptation after prior strain and sleep load. $confidenceClause"
            MetricType.SLEEP ->
                "Sleep quality drives hormonal repair and nervous-system recalibration, directly affecting next-day readiness. $confidenceClause"
            MetricType.STRAIN ->
                "Strain captures exercise load; keeping it aligned with recovery avoids cumulative fatigue and plateau risk. $confidenceClause"
            MetricType.STRESS ->
                "Stress reflects autonomic load over the day; sustained high values can suppress recovery and learning readiness. $confidenceClause"
        }
    }

    private fun buildWhatToDoNext(type: MetricType, score: Int): String = when (type) {
        MetricType.RECOVERY -> when {
            score >= 80 -> "Proceed with a quality training block and protect hydration plus post-session downregulation."
            score >= 60 -> "Use moderate intensity and keep total load capped to protect tomorrow's recovery."
            else -> "Prioritise recovery: low-intensity movement, sleep extension, and breath-focused downshift work."
        }
        MetricType.SLEEP -> when {
            score >= 80 -> "Maintain current sleep routine and pre-bed wind-down timing."
            score >= 60 -> "Reduce evening stimulation and target a consistent bedtime for the next 2 nights."
            else -> "Shift tonight toward recovery: earlier lights-out, lower caffeine, and cooler room temperature."
        }
        MetricType.STRAIN -> when {
            score >= 75 -> "Keep next session lighter or technique-focused to avoid overload carryover."
            score >= 50 -> "Maintain balanced training with one high-quality effort and sufficient recovery spacing."
            else -> "If training today, gradually ramp load to stay inside your adaptive range."
        }
        MetricType.STRESS -> when {
            score >= 70 -> "Use low-intensity activity and scheduled recovery breaks to reduce autonomic load."
            score >= 50 -> "Maintain regular movement and add short breathing resets during the day."
            else -> "You are in a stable range; maintain current routines and monitor evening recovery signals."
        }
    }

    private suspend fun buildMetricEvidence(
        type: MetricType,
        activeMetrics: DailyMetrics,
        trendMetrics: List<DailyMetrics>
    ): MetricEvidence {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now()
        val dayStartMs = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val nowMs = Instant.now().toEpochMilli()
        val sleepWindowStartMs = dayStartMs - 12 * 60 * 60 * 1000L

        val hrSamples = hrSampleDao.getRange(dayStartMs, nowMs)
        val hrvSamples = hrvSampleDao.getRange(dayStartMs, nowMs)
        val workouts = workoutSessionDao.getRange(dayStartMs, nowMs)
        val sleepSessions = sleepSessionDao
            .getRange(today.minusDays(1).toString(), today.toString())
            .filter { it.endTime >= sleepWindowStartMs && it.startTime <= nowMs }

        val latestSleep = sleepSessions.maxByOrNull { it.endTime }
        val previousDay = trendMetrics.dropLast(1).lastOrNull()

        return when (type) {
            MetricType.SLEEP -> {
                val sources = formatSources(
                    sleepSessions.map { it.sourcePackage } + "Daily score model"
                )
                val sleepStages = latestSleep?.let {
                    "deep ${it.deepMin}m, rem ${it.remMin}m, light ${it.lightMin}m, awake ${it.awakeMin}m"
                } ?: "Not available"

                MetricEvidence(
                    sources = sources,
                    dataPoints = listOf(
                        "Daily sleep score: ${activeMetrics.sleepScore}",
                        "Sleep duration used: ${formatMinutes(activeMetrics.sleepDurationMin)}",
                        "Sleep efficiency used: ${formatPercent(activeMetrics.sleepEfficiency)}",
                        "Latest session stages: $sleepStages",
                        "Sleep sessions considered: ${sleepSessions.size}"
                    )
                )
            }

            MetricType.RECOVERY -> {
                val sources = formatSources(
                    hrSamples.map { it.sourcePackage } +
                        hrvSamples.map { it.sourcePackage } +
                        sleepSessions.map { it.sourcePackage } +
                        "Daily score model"
                )

                MetricEvidence(
                    sources = sources,
                    dataPoints = listOf(
                        "HRV morning used: ${formatFloat(activeMetrics.hrvMorning, "ms")}",
                        "Resting HR morning used: ${formatFloat(activeMetrics.rhrMorning, "bpm")}",
                        "Sleep duration input: ${formatMinutes(activeMetrics.sleepDurationMin)}",
                        "Sleep efficiency input: ${formatPercent(activeMetrics.sleepEfficiency)}",
                        "Previous-day strain carryover: ${previousDay?.strainScore?.toString() ?: "Not available"}",
                        "Raw records used today: HR ${hrSamples.size}, HRV ${hrvSamples.size}, Sleep ${sleepSessions.size}"
                    )
                )
            }

            MetricType.STRAIN -> {
                val zones = deriveHeartRateZoneMinutes(
                    hrSamples = hrSamples,
                    workouts = workouts,
                    restingHr = activeMetrics.rhrMorning
                )
                val sources = formatSources(
                    hrSamples.map { it.sourcePackage } +
                        workouts.map { it.sourcePackage } +
                        "Daily score model"
                )

                MetricEvidence(
                    sources = sources,
                    dataPoints = listOf(
                        "Daily strain score: ${activeMetrics.strainScore}",
                        "Zone minutes used: Z1 ${formatZone(zones.zone1)}, Z2 ${formatZone(zones.zone2)}, Z3 ${formatZone(zones.zone3)}, Z4 ${formatZone(zones.zone4)}, Z5 ${formatZone(zones.zone5)}",
                        "Workouts considered: ${workouts.size}",
                        "Total active calories input: ${activeMetrics.activeCalories?.toString() ?: "Not available"}",
                        "Total distance input: ${formatDistance(activeMetrics.totalDistanceMeters)}"
                    )
                )
            }

            MetricType.STRESS -> {
                val hourlyBuckets = hrSamples.groupBy { sample ->
                    Instant.ofEpochMilli(sample.timestamp).atZone(zoneId).hour
                }
                val hourlyStress = hourlyBuckets.values.map { samples ->
                    val avgHr = samples.map { it.bpm }.average()
                    ((avgHr - 55.0) / 65.0 * 100.0).coerceIn(0.0, 100.0)
                }
                val highHours = hourlyStress.count { it >= 70.0 }
                val mediumHours = hourlyStress.count { it in 40.0..<70.0 }
                val lowHours = hourlyStress.count { it < 40.0 }
                val avgBpm = hrSamples.map { it.bpm }.average().takeIf { !it.isNaN() }
                val peakBpm = hrSamples.maxOfOrNull { it.bpm }

                val sources = formatSources(
                    hrSamples.map { it.sourcePackage } + "Daily score model"
                )

                MetricEvidence(
                    sources = sources,
                    dataPoints = listOf(
                        "Daily stress score: ${activeMetrics.stressScore}",
                        "HR samples considered: ${hrSamples.size}",
                        "Hourly windows considered: ${hourlyBuckets.size}",
                        "Estimated zone exposure: high ${highHours * 60}m, medium ${mediumHours * 60}m, low ${lowHours * 60}m",
                        "Heart-rate range used: avg ${avgBpm?.roundToInt() ?: "N/A"} bpm, peak ${peakBpm ?: "N/A"} bpm"
                    )
                )
            }
        }
    }

    private fun deriveHeartRateZoneMinutes(
        hrSamples: List<HrSample>,
        workouts: List<WorkoutSession>,
        restingHr: Float?
    ): ZoneMinutes {
        val workoutHrSignals = workouts
            .mapNotNull { workout ->
                val avgHr = workout.avgHr.takeIf { it > 0 }?.toFloat() ?: return@mapNotNull null
                val duration = workout.durationMin.takeIf { it > 0 }?.toFloat() ?: return@mapNotNull null
                avgHr to duration
            }

        if (hrSamples.isEmpty() && workoutHrSignals.isEmpty()) {
            return ZoneMinutes()
        }

        val baseResting = restingHr?.takeIf { it > 0f }
            ?: hrSamples.minOfOrNull { it.bpm }?.toFloat()
            ?: workoutHrSignals.minOfOrNull { it.first }
            ?: 60f

        val observedPeak = maxOf(
            hrSamples.maxOfOrNull { it.bpm }?.toFloat() ?: (baseResting + 40f),
            workoutHrSignals.maxOfOrNull { it.first } ?: (baseResting + 40f)
        )
        val zoneMaxHr = maxOf(observedPeak, baseResting + 30f)

        var z1 = 0f
        var z2 = 0f
        var z3 = 0f
        var z4 = 0f
        var z5 = 0f

        fun addMinutesToZone(bpm: Float, minutes: Float) {
            if (minutes <= 0f) return
            val reserve = (zoneMaxHr - baseResting).coerceAtLeast(1f)
            val intensity = ((bpm - baseResting) / reserve).coerceIn(0f, 1f)
            when {
                intensity < 0.60f -> z1 += minutes
                intensity < 0.70f -> z2 += minutes
                intensity < 0.80f -> z3 += minutes
                intensity < 0.90f -> z4 += minutes
                else -> z5 += minutes
            }
        }

        if (hrSamples.isNotEmpty()) {
            val sorted = hrSamples.sortedBy { it.timestamp }
            sorted.forEachIndexed { index, sample ->
                val intervalMinutes = if (index < sorted.lastIndex) {
                    ((sorted[index + 1].timestamp - sample.timestamp) / 60_000f).coerceIn(0f, 2f)
                } else {
                    0.5f
                }
                addMinutesToZone(sample.bpm.toFloat(), intervalMinutes)
            }
        }

        workoutHrSignals.forEach { (avgHr, durationMinutes) ->
            addMinutesToZone(avgHr, durationMinutes)
        }

        return ZoneMinutes(zone1 = z1, zone2 = z2, zone3 = z3, zone4 = z4, zone5 = z5)
    }

    private fun formatSources(rawSources: List<String>): List<String> {
        val normalized = rawSources
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { source ->
                when {
                    source == "Daily score model" -> source
                    source.equals("com.strava", ignoreCase = true) -> "Strava API"
                    source.contains("health", ignoreCase = true) -> "Health Connect ($source)"
                    source.contains("fit", ignoreCase = true) -> "Google Fit ($source)"
                    else -> source
                }
            }
            .distinct()
            .sorted()
            .toList()

        return if (normalized.isEmpty()) {
            listOf("No source metadata available")
        } else {
            normalized
        }
    }

    private fun formatMinutes(value: Int?): String {
        if (value == null) return "Not available"
        val hours = value / 60
        val mins = value % 60
        return if (hours > 0) "$hours h $mins m" else "$mins m"
    }

    private fun formatPercent(value: Float?): String {
        if (value == null) return "Not available"
        val normalized = if (value <= 1f) value * 100f else value
        return "${normalized.roundToInt()}%"
    }

    private fun formatFloat(value: Float?, unit: String): String {
        if (value == null) return "Not available"
        return "${String.format(Locale.US, "%.1f", value)} $unit"
    }

    private fun formatDistance(value: Float?): String {
        if (value == null) return "Not available"
        if (value >= 1000f) {
            return "${String.format(Locale.US, "%.2f", value / 1000f)} km"
        }
        return "${String.format(Locale.US, "%.0f", value)} m"
    }

    private fun formatZone(value: Float?): String {
        if (value == null || value <= 0f) return "0.0m"
        return "${String.format(Locale.US, "%.1f", value)}m"
    }

    private data class MetricEvidence(
        val sources: List<String>,
        val dataPoints: List<String>
    )

    private data class ZoneMinutes(
        val zone1: Float? = null,
        val zone2: Float? = null,
        val zone3: Float? = null,
        val zone4: Float? = null,
        val zone5: Float? = null
    )

    private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()
}
