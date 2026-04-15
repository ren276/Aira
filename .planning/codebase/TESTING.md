# Testing Strategy

Aira follows a multi-layered testing approach to ensure stability and data accuracy.

## Testing Stack

- **JUnit 5**: The default test runner for both unit and integration tests.
- **MockK**: Used for mocking dependencies and verifying interactions.
- **Turbine**: Facilitates testing of Kotlin Flows and StateFlows.
- **Kotlinx-Coroutines-Test**: Provides a `TestDispatcher` and `TestScope` for testing asynchronous code.
- **Compose UI Test**: Used for testing UI components and navigation.

## Unit Tests

- **Focus**: Domain Use Cases and ViewModel logic.
- **Pattern**: Given-When-Then structure.
- **Goals**: Verify state transitions and business rules in isolation.
- **Directory**: `app/src/test/java`.

## Integration Tests

- **Focus**: Repository implementations and Database DAOs.
- **Pattern**: Test against an in-memory Room database.
- **Goals**: Ensure correct data persistence and mapping from external sources.
- **Directory**: `app/src/androidTest/java`.

## UI & End-to-End Tests

- **Focus**: Critical user journeys (e.g., onboarding, score visualization).
- **Tooling**: `compose-ui-test` with Hilt-based UI testing.
- **Goals**: Verify the entire stack works together and UI elements respond correctly to user input.

## Data Validation

- **Nyquist Principle**: Ensure heart rate and other biometric data are sampled and processed at appropriate frequencies to avoid aliasing artifacts.
- **Mock Data**: Use generated physiological datasets for testing edge cases in score calculations.
