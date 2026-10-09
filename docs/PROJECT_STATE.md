# PROJECT STATE
Updated: 2026-10-10 00:34 IST   Branch: phase/03-auth-settings-sync   Last commit: WIP (ready for commit)
Current phase: 3 — Login (local-first), Settings, Session & Sync   Status: pending-human (automated gates G1-G3 green; ready for physical phone verification)
Done:
- Tagged `phase-02-done` on phase 2 acceptance
- Branched `phase/03-auth-settings-sync`
- Implemented Authentication screens & domain validation:
  * Pure Kotlin `AuthValidator` (Name, Email, Password, Password confirmation checks)
  * `AuthUiState` and `AuthViewModel` with `StateFlow` + `Channel<AuthEvent>` for single-activity navigation
  * `fragment_login.xml` with Material 3 styling: App branding, outlined inputs, sign in button, continue as guest, register link, loading bar, and error banners
  * `fragment_register.xml` with Material 3 styling: Outlined inputs with password toggles, name, email, confirm password, create account button, login link
  * Updated `LoginFragment` and `RegisterFragment` with reactive lifecycle bindings and field-level error rendering
- Implemented Settings screen & user preferences:
  * `SettingsUiState` and `SettingsViewModel` combining user session, theme, engine, and key status
  * `fragment_settings.xml` with Material 3 cards:
    - User profile card with avatar, name, email, and guest learner badge
    - Appearance card with real-time System/Light/Dark radio group
    - AI Engine card with Auto/Cloud/Local engine radio group and Gemini API key status indicator
    - Offline Model Manager navigation entry
    - Data & Storage card with "Clear Chat History" and confirmation dialog
    - Sign Out button with confirmation dialog
- Implemented Session Route Guard & Navigation shell:
  * BottomNavigationView and MaterialToolbar dynamically hidden on Login/Register screens
  * Single-activity reactive route guard in `MainActivity`: checks `authRepository.isLoggedIn` Flow without blocking the main thread, redirecting unauthenticated users to `LoginFragment`
  * Dynamic theme switching: `MainActivity` observes `settingsStore.themeMode` and applies `AppCompatDelegate.setDefaultNightMode()`
- Automated Unit Tests (55/55 passed):
  * `AuthValidatorTest`: 11 tests (valid name/email/passwords, boundary length, regex format, mismatch checks)
  * `AuthViewModelTest`: 7 tests (field errors, login success, error banner, register success, guest login, reset errors)
  * `SettingsViewModelTest`: 5 tests (state emission, theme change, engine preference, clear chat, logout)
  * `AuthRepositoryTest`: 9 tests
  * `PasswordHasherTest`: 5 tests
  * `ProgressCalculationTest`: 6 tests
  * `CurriculumLoaderTest`: 2 tests
  * `SearchRepositoryTest`: 2 tests
  * `UiTextTest`: 5 tests
  * `SanityUnitTest`: 3 tests
- Instrumented Tests:
  * `AuthSessionTest`: Instrumented test covering login, guest session, and logout
  * `NavigationTest`: Updated with session initialization
  * `RoomDatabaseTest`: In-memory persistence tests
- Verification gates:
  * G1 Build: assembleDebug (BUILD SUCCESSFUL)
  * G2 Lint: lintDebug (BUILD SUCCESSFUL, 0 errors)
  * G3 Unit tests: testDebugUnitTest (BUILD SUCCESSFUL, 55 passed)
  * G4 Instrumented: Ready for physical device execution over USB
  * G5 Physical phone smoke: Manual checklist provided (debug APK built)
  * G6 Monkey: NOT RUN on personal device per workflow rules
  * G7 Screenshots: Visual checks via physical phone manual inspection
In progress: None
Next:
- Awaiting human physical phone manual verification & acceptance of Phase 3
- Tag `phase-03-done` upon acceptance
- Start Phase 4: AI Abstraction + Gemini Cloud Chat (branch `phase/04-ai-gemini-chat`)
How to run (Physical-Device-First):
- G1 Build: `.\gradlew.bat --console=plain assembleDebug`
- G2 Lint: `.\gradlew.bat --console=plain lintDebug`
- G3 Unit tests: `.\gradlew.bat --console=plain testDebugUnitTest`
- Install to physical phone: `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk`
Known issues: None
Needs human: Manual checklist verification on physical device and acceptance of Phase 3
Key versions:
- AGP: 9.2.1
- Kotlin: built-in (KGP 2.2.10 runtime)
- KSP: 2.3.6
- Room: 2.7.0
- Navigation: 2.8.5
- Material: 1.12.0
- Gemini Model ID: gemini-2.5-flash (to be configured in AiConfig in Phase 4)
- LiteRT-LM: to be configured in Phase 5


