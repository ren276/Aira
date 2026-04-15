# Code Conventions & Standards

This document establishes the patterns and style guides for the Aira project.

## Kotlin Style

- **Functional Programming**: Prefer immutable states and functional transformations (e.g., `map`, `filter`, `fold`) over mutable collections and loops.
- **Naming**: Use standard camelCase for variables/functions and PascalCase for classes.
- **Extension Functions**: Use them to keep core classes clean and provide domain-specific functionality.
- **Null Safety**: Leverage Kotlin's null safety features. Avoid `!!` (double bang) unless strictly necessary.

## Jetpack Compose Patterns

- **State Hoisting**: Keep Composables stateless by hoisting state to ViewModels or parent Composables.
- **Slot API**: Build reusable UI components using slot parameters (`content: @Composable () -> Unit`).
- **Preview Parameters**: Provide `@PreviewParameter` providers for all UI components to enable realistic previews.
- **Theme Usage**: Always use `AiraTheme.colorScheme` or `MaterialTheme.colorScheme` instead of hardcoded hex values.

## DI (Hilt) Standards

- **Constructor Injection**: Always prefer constructor injection over field injection.
- **Interface Mapping**: Define Hilt modules to bind implementation classes to their domain repository interfaces.
- **Qualifiers**: Use `@ApplicationContext` or custom qualifiers for injecting ambiguous types (like distinct `DataStore` instances).

## Logging & Analytics

- **Timber**: Use for standard debug logging (if configured).
- **Firebase Analytics**: Use for tracking key user journeys in a privacy-preserving manner.
- **Errors**: Catch specific exceptions and log them to Firebase Crashlytics with additional context.

## Git Workflow

- **Branching**: Use descriptive branch names (e.g., `feature/onboarding-flow`, `fix/db-encryption`).
- **Commits**: Follow [Conventional Commits](https://www.conventionalcommits.org/) (e.g., `feat:`, `fix:`, `docs:`, `chore:`).
- **Planning**: All significant changes must be documented in a New/Update Plan artifact before implementation.
