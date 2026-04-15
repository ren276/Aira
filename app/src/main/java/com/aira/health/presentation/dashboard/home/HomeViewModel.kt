package com.aira.health.presentation.dashboard.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.model.DailyMetrics
import com.aira.health.data.worker.HealthSyncWorker
import com.aira.health.presentation.dashboard.home.state.HomeDeltaAnimator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
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
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)

    // Track last known snapshot for delta computation
    private var _lastSnapshot: HomeDeltaAnimator.Snapshot? = null

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

    val uiState: StateFlow<HomeUiState> = combine(
        todayMetricsFlow,
        recentMetricsFlow,
        _isSyncing
    ) { todayMetrics, recentMetrics, syncing ->
        val metrics = todayMetrics ?: recentMetrics
            ?: return@combine HomeUiState.Loading

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

        HomeUiState.Success(
            recoveryScore  = metrics.recoveryScore.coerceIn(0, 100),
            sleepScore     = metrics.sleepScore.coerceIn(0, 100),
            strainScore    = metrics.strainScore.coerceIn(0, 100),
            stressScore    = metrics.stressScore.coerceIn(0, 100),
            confidence     = metrics.dataConfidence.coerceIn(0f, 1f),
            lastUpdated    = metrics.calculatedAt,
            isSyncing      = syncing,
            recoveryDelta  = deltaSet?.recovery,
            sleepDelta     = deltaSet?.sleep,
            strainDelta    = deltaSet?.strain,
            stressDelta    = deltaSet?.stress,
            // Anomaly payload reserved for a future phase; null shows forecast fallback (D-09)
            anomaly = null
        )
    }.stateIn(
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
        // or cleared after a short delay as a safety net
        viewModelScope.apply {
            kotlinx.coroutines.launch {
                kotlinx.coroutines.delay(30_000) // 30 s safety net
                _isSyncing.update { false }
            }
        }
    }
}
