package com.aira.health.presentation.supplementary

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class NoRuntimeMockDataContractTest {

    private val disallowedPredictionAndCorrectionLiterals = setOf(
        "Aira Forecast",
        "95%",
        "Maintain light cardio tomorrow",
        "Sleep Offset Shift",
        "HRV Calibration"
    )

    private val disallowedStaticProfileAndCoachLiterals = setOf(
        "Aira User",
        "Gemma 3 2B (Running)",
        "Based on today's load and recovery"
    )

    @Test
    fun `prediction and correction seeded literals are blocked in production presentation files`() {
        val violations = findLiteralViolations(disallowedPredictionAndCorrectionLiterals)
        assertTrue(
            buildViolationMessage("prediction/correction", violations),
            violations.isEmpty()
        )
    }

    @Test
    fun `static profile and seeded coach prompt literals are blocked in production presentation files`() {
        val violations = findLiteralViolations(disallowedStaticProfileAndCoachLiterals)
        assertTrue(
            buildViolationMessage("profile/coach", violations),
            violations.isEmpty()
        )
    }

    @Test
    fun `quick add seeded defaults are blocked from production state files`() {
        val stateFiles = listOf(
            resolvePresentationRoot().resolve("train/TrainUiState.kt"),
            resolvePresentationRoot().resolve("nutrition/NutritionUiState.kt")
        )

        val disallowedDefaultFragments = setOf(
            "quickAddExercise: String = \"Strength Training\"",
            "quickAddDurationMin: String = \"45\""
        )

        val violations = mutableListOf<String>()
        stateFiles.filter { Files.exists(it) }.forEach { file ->
            val content = file.toFile().readText()
            disallowedDefaultFragments.forEach { fragment ->
                if (content.contains(fragment)) {
                    violations += "${file.toRelativePathString()} contains [$fragment]"
                }
            }
        }

        assertTrue(
            buildViolationMessage("quick-add defaults", violations),
            violations.isEmpty()
        )
    }

    private fun findLiteralViolations(disallowed: Set<String>): List<String> {
        val violations = mutableListOf<String>()
        presentationKotlinFiles().forEach { file ->
            val content = file.toFile().readText()
            disallowed.forEach { literal ->
                if (content.contains(literal)) {
                    violations += "${file.toRelativePathString()} contains [$literal]"
                }
            }
        }
        return violations
    }

    private fun presentationKotlinFiles(): List<Path> {
        val root = resolvePresentationRoot()
        if (!Files.exists(root)) return emptyList()

        val files = mutableListOf<Path>()
        Files.walk(root).use { stream ->
            stream
                .filter { Files.isRegularFile(it) }
                .filter { it.toString().endsWith(".kt") }
                .filter { !it.fileName.toString().contains("Preview", ignoreCase = true) }
                .forEach { files.add(it) }
        }
        return files
    }

    private fun resolvePresentationRoot(): Path {
        val moduleRelative = Paths.get("src/main/java/com/aira/health/presentation")
        if (Files.exists(moduleRelative)) return moduleRelative

        val rootRelative = Paths.get("app/src/main/java/com/aira/health/presentation")
        if (Files.exists(rootRelative)) return rootRelative

        return moduleRelative
    }

    private fun Path.toRelativePathString(): String {
        val cwd = Paths.get("").toAbsolutePath().normalize()
        return runCatching { cwd.relativize(this.toAbsolutePath().normalize()).toString() }
            .getOrDefault(this.toString())
            .replace('\\', '/')
    }

    private fun buildViolationMessage(scope: String, violations: List<String>): String {
        if (violations.isEmpty()) return "No $scope mock-data literal violations."
        return buildString {
            append("Found ")
            append(scope)
            append(" mock-data literals in production files:\n")
            violations.forEach { append("- ").append(it).append('\n') }
        }
    }
}
