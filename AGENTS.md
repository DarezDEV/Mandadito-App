# AGENTS.md - Development Guidelines for Mandadito App

This document provides comprehensive guidelines for agentic coding assistants working on the Mandadito Android application. Follow these guidelines to maintain code quality, consistency, and project standards.

## Project Overview

Mandadito is an Android delivery/marketplace app built with:
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository pattern
- **Backend**: Supabase (PostgreSQL + Auth + Storage + Edge Functions)
- **Local Database**: Room
- **Build System**: Gradle with Kotlin DSL
- **Minimum SDK**: 28 (Android 9.0)
- **Target SDK**: 36 (Android 12)

## Build, Lint, and Test Commands

### Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean and build
./gradlew clean assembleDebug

# Build specific module
./gradlew :app:assembleDebug
```

### Test Commands
```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run all instrumentation tests (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Run tests for specific module
./gradlew :app:testDebugUnitTest

# Run single test class
./gradlew testDebugUnitTest --tests "*AuthViewModelTest*"

# Run single test method
./gradlew testDebugUnitTest --tests "*AuthViewModelTest.testLoginSuccess*"

# Generate test coverage report
./gradlew createDebugCoverageReport
```

### Lint and Code Quality
```bash
# Run Android lint
./gradlew lintDebug

# Run lint on all variants
./gradlew lint

# Check code style (if ktlint configured)
./gradlew ktlintCheck

# Auto-fix code style issues
./gradlew ktlintFormat

# Run detekt static analysis (if configured)
./gradlew detekt
```

### Clean and Maintenance
```bash
# Clean build artifacts
./gradlew clean

# Clean and invalidate caches
./gradlew cleanBuildCache

# List available tasks
./gradlew tasks
```

## Code Style Guidelines

### Kotlin Language Features
- Use modern Kotlin syntax (data classes, sealed classes, etc.)
- Prefer `val` over `var` when possible
- Use named parameters for functions with multiple parameters
- Use default parameter values instead of method overloading
- Use extension functions for utility operations
- Prefer `when` expressions over if-else chains
- Use `apply`, `let`, `also`, `run` scope functions appropriately
- Use `require()` and `check()` for parameter validation

### Naming Conventions
- **Packages**: lowercase with dots (e.g., `com.dev.mandadito.data.models`)
- **Classes/Interfaces**: PascalCase (e.g., `AuthViewModel`, `ProductRepository`)
- **Functions/Methods**: camelCase (e.g., `getCurrentUser()`, `validateEmail()`)
- **Variables/Properties**: camelCase (e.g., `userName`, `isLoading`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_PASSWORD_LENGTH`)
- **Test Classes**: PascalCase ending with `Test` (e.g., `AuthViewModelTest`)
- **Test Methods**: camelCase starting with `test` (e.g., `testLoginSuccess()`)

### File Organization
```
app/src/main/java/com/dev/mandadito/
├── data/
│   ├── models/           # Data classes and DTOs
│   ├── repository/       # Repository implementations
│   ├── local/            # Room database entities and DAOs
│   ├── network/          # API services and network utilities
│   └── repository/       # Repository interfaces
├── presentation/
│   ├── viewmodels/       # ViewModels (organized by feature)
│   ├── navigation/       # Navigation components
│   └── screens/          # Composable screens (if not using navigation)
├── ui/
│   ├── theme/            # Theme, colors, typography
│   └── components/       # Reusable UI components
├── utils/                # Utility classes and extensions
├── config/               # Configuration and feature flags
└── MainActivity.kt       # Application entry point
```

### Architecture Patterns

#### MVVM Pattern
- **ViewModels**: Handle UI state and business logic
- **Views (Composables)**: Display UI and handle user interactions
- **Models**: Data classes representing domain objects
- **Repositories**: Abstract data access, handle caching and API calls

#### Repository Pattern
```kotlin
sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error<T>(val message: String) : Result<T>()
}

class AuthRepository(private val application: Application) {
    // Implementation
}
```

### Imports and Dependencies
- Group imports by package hierarchy
- Use wildcard imports sparingly (only for closely related classes)
- Prefer explicit imports over wildcard imports for clarity
- Order: Android SDK, third-party libraries, project imports
- Use alias imports when there are naming conflicts

```kotlin
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.dev.mandadito.data.models.User
import com.dev.mandadito.data.repository.AuthRepository
import kotlinx.coroutines.launch
```

### Error Handling
- Use try-catch blocks for expected exceptions
- Log errors with appropriate levels (DEBUG, INFO, WARN, ERROR)
- Provide user-friendly error messages
- Use sealed classes for operation results
- Handle network errors gracefully with retry mechanisms

```kotlin
try {
    val result = repository.login(email, password)
    when (result) {
        is AuthRepository.LoginResult.Success -> {
            // Handle success
        }
        is AuthRepository.LoginResult.Error -> {
            // Handle error with user-friendly message
            _uiState.value = _uiState.value.copy(error = result.message)
        }
    }
} catch (e: Exception) {
    Log.e(TAG, "Unexpected error during login", e)
    _uiState.value = _uiState.value.copy(
        error = "Connection error. Please check your internet and try again."
    )
}
```

### Logging Guidelines
- Use meaningful log tags (usually class name)
- Log important state changes and operations
- Use appropriate log levels:
  - `Log.d()`: Debug information (development only)
  - `Log.i()`: Important information
  - `Log.w()`: Warnings
  - `Log.e()`: Errors
- Include relevant context in log messages
- Remove or guard debug logs in production

```kotlin
companion object {
    private const val TAG = "AuthViewModel"
}

// Usage
Log.d(TAG, "Login successful for user: ${user.email}")
```

### Data Classes and Models
- Use `@Serializable` for Supabase communication
- Use `@SerialName` for custom field mapping
- Provide default values for optional fields
- Use meaningful property names

```kotlin
@Serializable
data class Product(
    val id: String,
    val name: String,
    val description: String? = null,
    val price: Double,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true
)
```

### ViewModels and State Management
- Use `StateFlow` for UI state
- Use `MutableStateFlow` internally, expose as `StateFlow`
- Handle loading states explicitly
- Clear errors on new operations
- Use `viewModelScope` for coroutines

```kotlin
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            // Perform login operation
        }
    }
}
```

### Compose UI Guidelines
- Use descriptive names for composables
- Prefer stateless composables when possible
- Use `remember` for state that survives recomposition
- Use `LaunchedEffect` for side effects
- Follow Material Design principles
- Use appropriate modifiers for layout and styling

```kotlin
@Composable
fun LoginButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        } else {
            Text("Login")
        }
    }
}
```

### Database and Room
- Use entities for database tables
- Use DAOs for database operations
- Use `@Transaction` for complex operations
- Use `Converters` for custom types
- Follow naming conventions for database objects

```kotlin
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val price: Double
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProduct(id: String): ProductEntity?
}
```

### Testing Guidelines
- Write unit tests for ViewModels, repositories, and utilities
- Use descriptive test method names
- Test both success and error scenarios
- Use `TestCoroutineDispatcher` for coroutine testing
- Mock dependencies using mockito or mockk

```kotlin
class AuthViewModelTest {
    @Test
    fun `login with valid credentials should succeed`() = runTest {
        // Given
        val mockRepository = mock<AuthRepository>()
        val viewModel = AuthViewModel(mockRepository)

        // When
        viewModel.login("test@example.com", "password")

        // Then
        // Assert expected behavior
    }
}
```

### Security Best Practices
- Never log sensitive information (passwords, tokens, keys)
- Use `BuildConfig` for API keys and configuration
- Validate all user inputs
- Use HTTPS for all network communications
- Store sensitive data securely using EncryptedSharedPreferences
- Implement proper authentication flows

### Performance Considerations
- Use `remember` in Compose for expensive computations
- Avoid blocking operations on main thread
- Use appropriate coroutine dispatchers
- Implement pagination for large lists
- Use `DiffUtil` for RecyclerView updates
- Profile and optimize database queries

### Documentation
- Document public APIs with KDoc comments
- Explain complex business logic
- Document parameters and return values
- Use `@param`, `@return`, and `@throws` tags

```kotlin
/**
 * Validates email format and length
 * @param email The email address to validate
 * @return ValidationResult.Success if valid, ValidationResult.Error with message if invalid
 */
fun isValidEmail(email: String): ValidationResult {
    // Implementation
}
```

### Git and Version Control
- Write clear, concise commit messages
- Use conventional commit format when possible
- Create feature branches for new work
- Keep commits focused and atomic
- Update this document when introducing new patterns

### Code Review Checklist
- [ ] Code follows established patterns and architecture
- [ ] Proper error handling and logging
- [ ] Unit tests written and passing
- [ ] No hardcoded strings or magic numbers
- [ ] Proper resource management (no memory leaks)
- [ ] Accessibility considerations
- [ ] Performance implications reviewed
- [ ] Security best practices followed

## Tool-Specific Guidelines

### Supabase Integration
- Use appropriate Supabase-KT libraries (auth-kt, postgrest-kt, storage-kt)
- Handle authentication state properly
- Implement proper error handling for network operations
- Use RLS (Row Level Security) policies
- Follow Supabase naming conventions for tables and columns

### Compose Navigation
- Use type-safe navigation with routes
- Pass minimal data through navigation arguments
- Handle back navigation properly
- Use nested navigation for complex flows

### Dependency Injection
- Use constructor injection when possible
- Consider Hilt for complex dependency graphs
- Avoid service locators and static dependencies

## Common Patterns and Anti-Patterns

### ✅ Do's
- Use sealed classes for result types
- Implement comprehensive validation
- Write descriptive error messages
- Use meaningful variable and function names
- Follow single responsibility principle
- Write tests for business logic
- Document complex algorithms

### ❌ Don'ts
- Don't use `!!` operator without proper null checks
- Don't hardcode strings in code (use strings.xml)
- Don't perform network operations on main thread
- Don't expose mutable state externally
- Don't ignore exceptions without proper handling
- Don't create god classes with multiple responsibilities
- Don't duplicate code (extract common functionality)

## Development Workflow

1. **Planning**: Understand requirements and identify affected components
2. **Implementation**: Follow established patterns and guidelines
3. **Testing**: Write and run unit tests, verify on device/emulator
4. **Code Review**: Ensure code quality and adherence to guidelines
5. **Integration**: Test with existing functionality
6. **Documentation**: Update this document for new patterns

Remember: Code is read more often than it's written. Prioritize readability, maintainability, and consistency over clever optimizations.