# Architectural & Technical Decisions Log

Every non-obvious choice (library, version, trade-off) must be recorded here with a one-line reason.

| Date | Decision | Reason |
|---|---|---|
| 2026-10-08 | Package `com.mit.tushar_kaldate` retained | Required by spec, matches project author namespace |
| 2026-10-08 | AGP 9.2.1 + Gradle 9.4.1 | Latest stable Android Gradle plugin and Gradle wrapper matched from local Android Studio environment |
| 2026-10-08 | AGP Built-in Kotlin (KGP 2.2.10 runtime) | AGP 9 default; simplifies build scripts and avoids external KGP plugin conflicts |
| 2026-10-08 | KSP 2.3.6 | Required for AGP 9 built-in Kotlin compatibility (removes legacy kotlin.sourceSets reference) |
| 2026-10-08 | Manual `AppContainer` DI (No Hilt) | Avoids annotation processor / KSP DI overhead and compilation complexity during hackathon loops |
| 2026-10-08 | XML Views + ViewBinding (No Jetpack Compose) | Explicit constraint from spec; guaranteed single-activity ViewBinding stability |
| 2026-10-08 | CompileSdk 35 / MinSdk 26 | Modern Android API features while supporting devices back to Android 8.0 (API 26) |
| 2026-10-08 | AndroidJUnitRunner + ExampleInstrumentedTest | Verification of application context and runtime APK installation on Pixel 9 AVD |
| 2026-10-08 | Material 3 Day/Night theme system | Built using 4dp dimensional spacing grid and standard color roles |
| 2026-10-08 | Single-Activity NavHostFragment with 13 destinations | Follows master spec architecture with ViewBinding on every Fragment |
| 2026-10-08 | Reusable Common Views (Loading, Empty, Error, CodeBlock) | Provides standardized UI feedback states across all screens with clipboard copying |
| 2026-10-08 | ViewCompat WindowInsetsCompat on MainActivity topAppBar | Fixes Android 15 edge-to-edge overlap with status bar in toolbar |
| 2026-10-08 | animationsDisabled = true in testOptions | Disables window and transition animations during connected Android tests for Espresso stability |
| 2026-10-09 | Room 2.7.0 update | Resolves KSP 2.3.6 JVM signature bug with Void/Unit return types present in Room 2.6.1 |
| 2026-10-09 | `PasswordHasher` uses `java.util.Base64` | Enables standard JVM unit test execution without mocking `android.util.Base64` |
| 2026-10-09 | Physical-Device-First Verification | Mitigates host CPU overheating by eliminating default emulator execution and using physical phone checklists |
| 2026-10-09 | Escaped path in `gradle.properties` | `C\:/...` required by Android Lint `PropertyEscape` check while binding Gradle daemon to JDK 21 |
| 2026-10-10 | Pure Kotlin Regex in `AuthValidator` | Replaces `android.util.Patterns` to allow pure JVM unit testing without framework mocks |
| 2026-10-10 | Open `AuthRepository` + `FakeAuthRepository` in tests | Avoids MockK reflection `ClassCastException` with Kotlin inline value class `Result` in coroutines |
| 2026-10-10 | Single-Activity Route Guard via `isLoggedIn` Flow | Replaces blocking route checks with coroutine flow collection to never block the main thread |
| 2026-10-10 | Reactive Theme Application | Observes DataStore `themeMode` and dynamically calls `AppCompatDelegate.setDefaultNightMode()` |
| 2026-10-10 | `RadioButton.setOnClickListener` in Settings | Prevents infinite loop where `RadioGroup.setOnCheckedChangeListener` was re-triggered by programmatic DataStore state updates and Activity recreation |
| 2026-10-10 | REST SSE streaming with OkHttp | Server-Sent Events over OkHttp enables streaming tokens from Gemini v1beta streamGenerateContent without heavy proprietary SDK dependencies |
| 2026-10-10 | Coroutine-linked OkHttp cancellation | Invoking `call.cancel()` in `channelFlow.invokeOnClose` aborts HTTP socket immediately (<1s) upon user stop generation |
| 2026-10-10 | Markwon + TablePlugin for Chat Bubbles | Native Markdown formatting and tables in Android TextViews without WebViews |
| 2026-10-10 | Transient streaming UI model with -1 ID | Smooth token-by-token UI updates while streaming, persisting exactly once to Room upon completion or user cancellation |
| 2026-10-10 | Evergreen `gemini-flash-latest` Cloud Model | Replaces deprecated `gemini-2.5-flash` (which returned 404 for new keys) with active, stable `gemini-flash-latest` |
| 2026-10-10 | 3-Tier Offline LLM Catalog | Standardizes offline models into Quality (Gemma 2 2B), Standard (Qwen 2.5 1.5B), and Lite (Qwen 2.5 0.5B) tiers matching Android device hardware capabilities |
| 2026-10-10 | WorkManager HTTP Range Downloader | Enables resumable model downloads with background persistence, Wi-Fi constraints, and foreground notifications without bundling models in the APK |
| 2026-10-10 | `usableSpace` + JVM fallback in `DeviceCapabilityChecker` | Allows robust hardware evaluation on both real Android hardware and host JVM test environments |
| 2026-10-10 | Application `onTrimMemory` model unloading | Automatically closes and unloads heavy model weights when Android triggers TRIM_MEMORY_RUNNING_CRITICAL to prevent LMK crashes |
| 2026-10-10 | Dynamic `MaterialColors.getColor` in Adapter | Automatically resolves active Material 3 theme colors across day/night modes in custom RecyclerView holders |
| 2026-10-10 | Offline Tutor Knowledge Base (`OfflineTutorKnowledgeBase`) | Replaces canned stub responses in LiteRtLocalEngine with rich, domain-aware algorithmic explanations, syntax-tagged code in Java/Python/C++/Kotlin, and Big-O analysis for on-device chat |
| 2026-10-10 | Official `litertlm-android:0.17.1` Runtime Integration | Integrates native Google LiteRT-LM runtime (`Engine`, `Conversation`, `liblitertlm_jni.so`) with Kotlin 2.2.10 stdlib alignment and graceful fallback to `OfflineTutorKnowledgeBase` |
