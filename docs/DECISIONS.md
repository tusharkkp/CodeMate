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

