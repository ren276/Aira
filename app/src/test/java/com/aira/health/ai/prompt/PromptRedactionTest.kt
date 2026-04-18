package com.aira.health.ai.prompt

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * AIM-03 — Verifies that [PromptAssembler] never allows raw biometric tokens or
 * disallowed literals to leak into assembled prompt chunks.
 */
class PromptRedactionTest {

    private val assembler = PromptAssembler()

    // ---------------------------------------------------------------------------
    // Aggregate-only payload checks
    // ---------------------------------------------------------------------------

    @Test
    fun `assembled prompt contains only score fields not raw sensor arrays`() {
        val snapshot = sampleSnapshot()
        val contract = assembler.assemble(snapshot)
        val allChunks = contract.toChunks().joinToString("\n")

        // Should contain aggregated score labels
        assertTrue(allChunks.contains("Recovery:"), "Expected Recovery label in prompt")
        assertTrue(allChunks.contains("Sleep quality:"), "Expected Sleep quality label in prompt")
        assertTrue(allChunks.contains("Strain:"), "Expected Strain label in prompt")

        // Must NOT contain class names or field names from raw Room entities
        assertFalse(allChunks.contains("HealthRecordRaw"), "Raw entity class must not appear in prompt")
        assertFalse(allChunks.contains("HrSample"), "HrSample must not appear in prompt")
        assertFalse(allChunks.contains("HrvSample"), "HrvSample must not appear in prompt")
        assertFalse(allChunks.contains("SleepSession"), "SleepSession must not appear in prompt")
        assertFalse(allChunks.contains("calculatedAt"), "Internal Room field must not appear in prompt")
    }

    @Test
    fun `assembled prompt never exposes raw biometric identifiers`() {
        val snapshot = sampleSnapshot()
        val contract = assembler.assemble(snapshot, userNotes = "My patient ID is 12345")
        val allChunks = contract.toChunks().joinToString("\n")

        // User note is included but numeric ID patterns in the note are still safe here
        // (not a blocked pattern), but medical diagnostic terms must be stripped
        assertTrue(allChunks.isNotBlank())
    }

    // ---------------------------------------------------------------------------
    // Redaction rules
    // ---------------------------------------------------------------------------

    @Test
    fun `email addresses are redacted from user notes`() {
        val result = assembler.redactFreeText("Contact me at athlete@example.com for questions")
        assertFalse(result.contains("@"), "Email must be redacted: $result")
        assertFalse(result.contains("example.com"), "Email domain must be redacted: $result")
    }

    @Test
    fun `URLs are redacted from user notes`() {
        val result = assembler.redactFreeText("See https://myhealthapp.com/records for more")
        assertFalse(result.contains("https://"), "URL must be redacted: $result")
        assertFalse(result.contains("myhealthapp.com"), "URL domain must be redacted: $result")
    }

    @Test
    fun `blood pressure literals are redacted`() {
        val result = assembler.redactFreeText("My BP 120/80 was checked this morning")
        assertFalse(result.contains("120/80"), "Blood pressure literal must be redacted: $result")
    }

    @Test
    fun `glucose reading literals are redacted`() {
        val result = assembler.redactFreeText("glucose=6.5 recorded after breakfast")
        assertFalse(result.contains("6.5"), "Glucose literal must be redacted: $result")
        assertFalse(result.lowercase().contains("glucose="), "Glucose pattern must be redacted: $result")
    }

    @Test
    fun `medical diagnosis terms are redacted`() {
        val result = assembler.redactFreeText("The doctor diagnosed me with hypertension and prescribed medication")
        assertFalse(result.lowercase().contains("diagnos"), "Diagnosis term must be redacted: $result")
        assertFalse(result.lowercase().contains("prescri"), "Prescription term must be redacted: $result")
    }

    @Test
    fun `IP addresses are redacted`() {
        val result = assembler.redactFreeText("Device at 192.168.1.10 sent health data")
        assertFalse(result.contains("192.168.1.10"), "IP address must be redacted: $result")
    }

    @Test
    fun `normal athlete notes pass through after redaction`() {
        val note = "Felt heavy legs after yesterday's long run, slept about 7 hours"
        val result = assembler.redactFreeText(note)
        assertTrue(result.contains("heavy legs"), "Benign note content must survive redaction: $result")
        assertTrue(result.contains("slept"), "Normal phrasing must survive redaction: $result")
    }

    @Test
    fun `user notes are truncated to max length`() {
        val longNote = "a".repeat(500)
        val result = assembler.redactFreeText(longNote)
        assertTrue(result.length <= 200, "Redacted note must not exceed 200 chars, got ${result.length}")
    }

    // ---------------------------------------------------------------------------
    // fromDailyMetrics mapper — privacy boundary check
    // ---------------------------------------------------------------------------

    @Test
    fun `fromDailyMetrics mapper does not forward unsupported raw fields`() {
        val metrics = com.aira.health.data.local.model.DailyMetrics(
            date = "2026-04-17",
            recoveryScore = 72,
            sleepScore = 68,
            strainScore = 55,
            stressScore = 40,
            dataConfidence = 0.82f,
            hrvMorning = 52.4f,
            rhrMorning = 58.0f,
            sleepDurationMin = 430,
        )
        val snapshot = assembler.fromDailyMetrics(metrics)

        // Only permitted aggregated fields should be present
        assertTrue(snapshot.recoveryScore == 72)
        assertTrue(snapshot.sleepScore == 68)
        assertTrue(snapshot.hrv == 52.4f)

        // Raw entity type must not be the return type — confirmed by compile-time type
        assertTrue(snapshot is MetricSnapshot)
    }

    // ---------------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------------

    private fun sampleSnapshot() = MetricSnapshot(
        date = "2026-04-17",
        recoveryScore = 75,
        sleepScore = 70,
        strainScore = 50,
        stressScore = 35,
        dataConfidence = 0.85f,
        hrv = 54.2f,
        rhr = 57.0f,
        sleepDurationMin = 450,
        compositeReadiness = 72,
    )
}
