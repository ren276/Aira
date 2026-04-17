package com.aira.health.presentation.dashboard.home.state

import com.aira.health.presentation.dashboard.home.ScoreDelta

/**
 * Produces [ScoreDelta] payloads by comparing a previous snapshot against current scores.
 *
 * Policy rules (from D-08):
 *  - Only emits a delta when the value actually changed.
 *  - Returns null for unchanged scores so the UI does not animate needlessly.
 *  - All math is pure — safe to call from coroutine context or test.
 */
object HomeDeltaAnimator {

    data class Snapshot(
        val recovery: Int,
        val sleep: Int,
        val strain: Int,
        val stress: Int
    )

    data class DeltaSet(
        val recovery: ScoreDelta?,
        val sleep: ScoreDelta?,
        val strain: ScoreDelta?,
        val stress: ScoreDelta?
    )

    /**
     * Compares [previous] vs [current] and returns a [DeltaSet] where each field is non-null
     * only if the corresponding score changed.
     */
    fun compute(previous: Snapshot, current: Snapshot): DeltaSet = DeltaSet(
        recovery = deltaOrNull(previous.recovery, current.recovery),
        sleep    = deltaOrNull(previous.sleep,    current.sleep),
        strain   = deltaOrNull(previous.strain,   current.strain),
        stress   = deltaOrNull(previous.stress,   current.stress)
    )

    private fun deltaOrNull(prev: Int, curr: Int): ScoreDelta? =
        if (prev != curr) ScoreDelta(previous = prev, current = curr) else null
}
