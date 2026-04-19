package com.aira.health.domain.engine

import com.aira.health.domain.model.CausalDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CausalRankingEngineTest {

    private val engine = CausalRankingEngine()

    @Test
    fun `ranking returns exactly top three factors by contribution weight`() {
        val ranked = engine.rankTopFactors(
            listOf(
                CausalRankingEngine.Candidate("sleep_efficiency", 0.40f, "7d", 10L),
                CausalRankingEngine.Candidate("hrv_morning", 0.80f, "24h", 20L),
                CausalRankingEngine.Candidate("stress_load", 0.60f, "72h", 30L),
                CausalRankingEngine.Candidate("strain_pressure", 0.20f, "72h", 40L)
            )
        )

        assertEquals(3, ranked.size)
        assertEquals("hrv_morning", ranked[0].key)
        assertEquals("stress_load", ranked[1].key)
        assertEquals("sleep_efficiency", ranked[2].key)
        assertTrue(ranked.all { it.weight in 0f..1f })
    }

    @Test
    fun `direction and weight fields have stable sign behavior`() {
        val ranked = engine.rankTopFactors(
            listOf(
                CausalRankingEngine.Candidate("positive", 0.4f, "24h", 20L),
                CausalRankingEngine.Candidate("negative", -0.3f, "72h", 21L),
                CausalRankingEngine.Candidate("neutral", 0.001f, "7d", 22L)
            )
        )

        val byKey = ranked.associateBy { it.key }
        assertEquals(CausalDirection.INCREASED, byKey["positive"]?.direction)
        assertEquals(CausalDirection.DECREASED, byKey["negative"]?.direction)
        assertEquals(CausalDirection.NEUTRAL, byKey["neutral"]?.direction)
    }

    @Test
    fun `near equal weights tie break by recency then key ascending`() {
        val ranked = engine.rankTopFactors(
            listOf(
                CausalRankingEngine.Candidate("b_factor", 0.50f, "24h", 100L),
                CausalRankingEngine.Candidate("a_factor", 0.52f, "72h", 100L),
                CausalRankingEngine.Candidate("recent_factor", 0.51f, "7d", 120L),
                CausalRankingEngine.Candidate("older_factor", 0.49f, "7d", 90L)
            )
        )

        assertEquals("recent_factor", ranked[0].key)
        assertEquals("a_factor", ranked[1].key)
        assertEquals("b_factor", ranked[2].key)
    }
}
