# Testing Patterns

**Analysis Date:** 2026-04-15

## Test Framework

**Runner:**

- JUnit 5.11.3 via `testOptions { unitTests.all { it.useJUnitPlatform() } }` in `app/build.gradle.kts`.
- `junit-jupiter-api`, `junit-jupiter-engine`, and `junit-platform-launcher` are declared in the version catalog.

**Assertion Library:**

- JUnit Jupiter assertions are used directly.

**Supporting Libraries:**

- MockK for mocking and verification.
- `kotlinx-coroutines-test` for suspending code.
- Turbine is declared in `gradle/libs.versions.toml`, but no checked-in test currently uses it.
- Compose UI test dependencies are declared, but no checked-in instrumentation test currently uses them.

**Run Commands:**

```bash
./gradlew :app:testDevDebugUnitTest
./gradlew :app:testStagingDebugUnitTest
./gradlew :app:testProdDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

## Test File Organization

**Location:**

- Current tests are colocated under `app/src/test/java/com/aira/health/...` and mirror the production package structure.
- No checked-in `app/src/androidTest` directory is present in the workspace.

**Naming:**

- Test files end with `Test.kt`, for example `UserRepositoryImplTest.kt` and `IngestHealthDataUseCaseTest.kt`.

**Structure:**

```text
app/src/test/java/com/aira/health/
	data/
		model/
		repository/
		worker/
	di/
	domain/
		model/
		usecase/
	util/
		receiver/
```

## Test Structure

**Suite Organization:**

- Use one test class per production class or pure helper.
- Keep setup local to the file unless the same fixture is reused across multiple tests.

**Observed Pattern:**

```kotlin
class IngestHealthDataUseCaseTest {

		@Test
		fun `invoke resolves overlaps and persists sync timestamp`() = runTest {
				val repository = mockk<HealthDataRepository>()
				val hrDao = mockk<HrSampleDao>(relaxed = true)
				...
				coEvery { repository.readHeartRate(any(), any()) } returns hr
				...
				val ingestedCount = useCase.invoke()
				...
				coVerify(exactly = 1) { hrDao.insertAll(match { it.size == 2 }) }
		}
}
```

**Patterns:**

- Follow a Given-When-Then flow inside each test.
- Build data inline with explicit values so failures are easy to diagnose.
- Use `runTest` around suspend functions instead of blocking the main thread.

## Mocking

**Framework:**

- MockK.

**Patterns:**

```kotlin
val repository = mockk<HealthDataRepository>()
coEvery { repository.readSleepSessions(any(), any()) } returns sleep
coVerify(exactly = 1) { sleepDao.insert(match { it.confidence == 0.9f }) }

mockkStatic(WorkManager::class)
every { WorkManager.getInstance(context) } returns workManager

mockkObject(HealthSyncWorker.Companion)
every { HealthSyncWorker.schedule(context) } just runs
```

**What to Mock:**

- Android framework objects such as `Context`, `Intent`, and `WorkManager`.
- SDK clients such as Supabase and Health Connect when the test is about orchestration or mapping.
- DAOs and repository dependencies.

**What NOT to Mock:**

- Pure value objects and deterministic helpers such as `AuthState`, `UserSession`, and `ConfidenceRouter`.

**Cleanup:**

- Call `unmockkAll()` in `@AfterEach` when a test uses `mockkStatic` or `mockkObject`.

## Fixtures and Factories

**Test Data:**

- Fixtures are built inline in each test file rather than in a shared factory module.
- `PreferenceDataStoreFactory.create` with `Files.createTempFile(...)` is used to isolate DataStore state in unit tests.

**Location:**

- No central fixture directory is present.

## Coverage

**Requirements:**

- No coverage threshold or coverage plugin is detected.

**View Coverage:**

- No dedicated coverage task is configured in the current Gradle files.

## Test Types

**Unit Tests:**

- This is the only checked-in test type currently present.
- Coverage includes repository mapping, use case orchestration, worker scheduling, boot receiver behavior, DI selection, and model helpers.

**Integration Tests:**

- Not currently checked in.
- `app/src/androidTest` is absent even though `androidTestImplementation` dependencies exist in `app/build.gradle.kts`.

**E2E Tests:**

- Not currently checked in.
- Compose UI test dependencies are declared, but no Compose UI test sources exist.

## Common Patterns

**Async Testing:**

```kotlin
@Test
fun `signInAsGuest returns expected local guest session`() = runTest {
		val repository = createRepository(isGuestMode = true)

		val result = repository.signInAsGuest()

		assertTrue(result.isSuccess)
}
```

**Error Testing:**

- Assert failure modes through explicit `Result` checks, state objects, or empty collections.
- Prefer deterministic fallback behavior over exception-heavy tests when the production code intentionally soft-fails.

**Scheduling/Static APIs:**

- Use `mockkStatic` for `WorkManager.getInstance(...)` and `mockkObject` for singleton companions when verifying scheduling side effects.

---

_Testing analysis: 2026-04-15_
