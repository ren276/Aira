package com.aira.health.presentation.dashboard.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.BuildConfig
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.model.DailyMetrics
import com.aira.health.data.worker.HealthSyncWorker
import com.aira.health.domain.model.AuthState
import com.aira.health.domain.repository.UserRepository
import com.aira.health.presentation.dashboard.home.state.HomeDeltaAnimator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * Home dashboard ViewModel.
 *
 * Behaviour contract (D-08):
 *  1. Subscribes to [DailyMetricsDao.observeByDate] for today — emits cached state immediately.
 *  2. Falls back to [DailyMetricsDao.observeRecent] on first run when today has no row.
 *  3. Refresh triggers [HealthSyncWorker.scheduleImmediate] — non-blocking, does not clear UI.
 *  4. When a sync causes a new emission from Room, [HomeDeltaAnimator] computes deltas.
 *  5. [isSyncing] tracks debounced refresh state (no spinner-first loading when cache exists).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dailyMetricsDao: DailyMetricsDao,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private companion object {
        const val TAG = "AiraHomeDebug"
    }

    private val _isSyncing = MutableStateFlow(false)

    // Track last known snapshot for delta computation
    private var _lastSnapshot: HomeDeltaAnimator.Snapshot? = null
    private var initialSyncTriggered = false

    /** Today's date string in "YYYY-MM-DD" format */
    private val today: String get() = LocalDate.now().toString()

    /** Primary flow: today's metrics from Room */
    private val todayMetricsFlow: StateFlow<DailyMetrics?> =
        dailyMetricsDao.observeByDate(today)
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    /**
     * Fallback: most recent metrics row when today has no data.
     * Room emits immediately from its in-memory cache.
     */
    private val recentMetricsFlow: StateFlow<DailyMetrics?> =
        dailyMetricsDao.observeRecent(limit = 1)
            .map { it.firstOrNull() }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    private val historyFlow: StateFlow<List<DailyMetrics>> =
        dailyMetricsDao.observeRecent(7)
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private val authStateFlow: StateFlow<AuthState> =
        userRepository.observeAuthState()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AuthState.Loading
            )

    init {
        // Keep periodic sync registered on normal app startup (not only reboot).
        HealthSyncWorker.schedule(context)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        todayMetricsFlow,
        recentMetricsFlow,
        historyFlow,
        _isSyncing,
        authStateFlow
    ) { todayMetrics, recentMetrics, history, syncing, authState ->
        val hour = LocalTime.now().hour
        val greeting = when {
            hour < 12 -> "Morning"
            hour < 17 -> "Afternoon"
            else -> "Evening"
        }

        val userName = when (authState) {
            is AuthState.Authenticated -> {
                authState.session.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: authState.session.email
                        ?.substringBefore('@')
                        ?.takeIf { it.isNotBlank() }
                    ?: "Athlete"
            }
            AuthState.Guest -> "Guest"
            else -> "Athlete"
        }

        val metrics = todayMetrics ?: recentMetrics
            ?: run {
                if (!initialSyncTriggered) {
                    initialSyncTriggered = true
                    requestRefresh()
                }

                return@combine HomeUiState.Empty(
                    message = if (syncing) {
                        "Syncing your health data..."
                    } else {
                        "No synced health records yet. Open Health Connect and verify your data sources are connected, then pull to refresh."
                    },
                    userName = userName,
                    greeting = greeting,
                    statusHeadline = "Sync required",
                    isSyncing = syncing
                )
            }

        val currentSnapshot = HomeDeltaAnimator.Snapshot(
            recovery = metrics.recoveryScore,
            sleep    = metrics.sleepScore,
            strain   = metrics.strainScore,
            stress   = metrics.stressScore
        )

        val deltaSet = _lastSnapshot?.let {
            HomeDeltaAnimator.compute(it, currentSnapshot)
        }
        _lastSnapshot = currentSnapshot

        val statusHeadline = when {
            metrics.recoveryScore >= 80 -> "Optimal."
            metrics.recoveryScore >= 50 -> "Balanced."
            else -> "Recovering."
        }

        val anomalyPayload = when {
            metrics.recoveryScore <= 40 && metrics.stressScore >= 70 -> AnomalyPayload(
                title = "Recovery suppression detected",
                description = "Recovery is low while stress is elevated. Reduce load today and prioritize downregulation.",
                severity = 0.82f
            )
            metrics.sleepScore <= 45 -> AnomalyPayload(
                title = "Sleep debt signal",
                description = "Sleep quality is below your normal range. Expect reduced adaptation capacity today.",
                severity = 0.62f
            )
            else -> null
        }

        HomeUiState.Success(
            recoveryScore  = metrics.recoveryScore.coerceIn(0, 100),
            sleepScore     = metrics.sleepScore.coerceIn(0, 100),
            strainScore    = metrics.strainScore.coerceIn(0, 100),
            stressScore    = metrics.stressScore.coerceIn(0, 100),
            confidence     = metrics.dataConfidence.coerceIn(0f, 1f),
            lastUpdated    = metrics.calculatedAt,
            isSyncing      = syncing,
            userName       = userName,
            greeting       = greeting,
            statusHeadline = statusHeadline,
            energyBankPct  = metrics.energyBankScore.coerceIn(0, 100),
            totalSteps     = metrics.totalSteps,
            activeCalories = metrics.activeCalories,
            rhr            = metrics.rhrMorning?.toInt(),
            hrv            = metrics.hrvMorning?.toInt(),
            spo2           = metrics.spo2?.toInt(),
            temp           = metrics.skinTemperature,
            sleepDurationHours = metrics.sleepDurationMin?.let { it / 60f },
            rhrHistory     = history.mapNotNull { it.rhrMorning },
            hrvHistory     = history.mapNotNull { it.hrvMorning },
            spo2History    = history.mapNotNull { it.spo2 },
            tempHistory    = history.mapNotNull { it.skinTemperature },
            recoveryHistory = history.map { it.recoveryScore.toFloat() },
            sleepHistory    = history.map { it.sleepScore.toFloat() },
            strainHistory   = history.map { it.strainScore.toFloat() },
            stressHistory   = history.map { it.stressScore.toFloat() },
            recoveryDelta  = deltaSet?.recovery,
            sleepDelta     = deltaSet?.sleep,
            strainDelta    = deltaSet?.strain,
            stressDelta    = deltaSet?.stress,
            anomaly        = anomalyPayload
        )
    }
    .onEach { state ->
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Home UI emission -> ${state.toDebugSummary()}")
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading
    )

    /**
     * Triggers a non-blocking fast-sync via WorkManager.
     *
     * Debounce note: [HealthSyncWorker.scheduleImmediate] uses [ExistingWorkPolicy.REPLACE],
     * so rapid calls naturally coalesce into a single enqueued request (T-04-05).
     */
    fun requestRefresh() {
        // Only flip syncing flag — do NOT clear existing UI data (D-08)
        _isSyncing.update { true }
        HealthSyncWorker.scheduleImmediate(context)
        // Syncing flag is reset when Room emits a fresh row from the worker result
        // or cleared after a short delay as a safety net (T-04-05)
        viewModelScope.launch {
            delay(30_000) // 30 s safety net
            _isSyncing.update { false }
        }
    }

    private fun HomeUiState.toDebugSummary(): String = when (this) {
        HomeUiState.Loading -> "Loading"
        is HomeUiState.Empty -> "Empty(message=$message, user=$userName, greeting=$greeting, headline=$statusHeadline, syncing=$isSyncing)"
        is HomeUiState.Error -> "Error(message=$message)"
        is HomeUiState.Success -> buildString {
            append("Success(recovery=")
            append(recoveryScore)
            append(", sleep=")
            append(sleepScore)
            append(", strain=")
            append(strainScore)
            append(", stress=")
            append(stressScore)
            append(", confidence=")
            append(confidence)
            append(", lastUpdated=")
            append(lastUpdated)
            append(", energyBank=")
            append(energyBankPct)
            append(", steps=")
            append(totalSteps)
            append(", calories=")
            append(activeCalories)
            append(", rhr=")
            append(rhr)
            append(", hrv=")
            append(hrv)
            append(", spo2=")
            append(spo2)
            append(", temp=")
            append(temp)
            append(", sleepHours=")
            append(sleepDurationHours)
            append(", recoveryHistorySize=")
            append(recoveryHistory.size)
            append(", sleepHistorySize=")
            append(sleepHistory.size)
            append(", strainHistorySize=")
            append(strainHistory.size)
            append(", stressHistorySize=")
            append(stressHistory.size)
            append(", anomaly=")
            append(anomaly?.title ?: "null")
            append(')')
        }
    }
}
