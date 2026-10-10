# PROJECT STATE
Updated: 2026-10-10 03:14 IST   Branch: phase/05-on-device-llm   Last commit: e7a8e8a fix(ai): resolve model download stalls and live progress observation
Current phase: 5 — On-Device Offline LLM & Model Manager (LiteRT-LM)   Status: pending-human (automated gates G1-G3 green; ready for physical phone verification)
Done:
- Tagged `phase-04-done` on phase 4 acceptance
- Created and switched to branch `phase/05-on-device-llm`
- Implemented On-Device Hardware Tier Catalog (`ModelCatalog.kt`):
  * `QUALITY` tier: `gemma-2-2b-it` (~2.5 GB, min 6 GB RAM, Gemma Terms, verified live HuggingFace CDN link)
  * `STANDARD` tier: `qwen2.5-1.5b-instruct` (~557 MB, min 4 GB RAM, Apache-2.0, verified live HuggingFace CDN link)
  * `LITE` tier: `qwen2.5-0.5b-instruct` / Gemma 270M (~272 MB, min 3 GB RAM, Apache-2.0, verified live HuggingFace CDN link)
- Implemented Device Capability & Safety Inspector (`DeviceCapabilityChecker.kt`):
  * Dynamic inspection of physical total RAM, available RAM, and free filesystem space
  * Hardware recommendation logic recommending optimal tier for device specs
  * OOM prevention warning when RAM < 2.5 GB or free storage < 800 MB
- Implemented On-Device Storage & Model Manager (`ModelManager.kt`):
  * Sandboxed model directory management in `getExternalFilesDir("models")`
  * Detection of side-loaded `.litertlm`, `.bin`, and `.task` files
  * SAF file import via `importModelFromUri()`
  * Safe model deletion and SHA-256 integrity verification
- Implemented Resumable Background Downloader (`ModelDownloadWorker.kt`):
  * Background download worker using WorkManager and OkHttp
  * Resumable downloads via HTTP `Range` requests and `.part` temporary files
  * Live progress reporting via `setProgress()` and foreground service notifications
  * Wi-Fi only network constraints support
- Implemented LiteRT On-Device LLM Engine (`LiteRtLocalEngine.kt`):
  * Implements universal `LlmEngine` interface (`type = EngineType.LOCAL`)
  * Off main thread execution on `Dispatchers.IO`
  * Responsive token streaming with realistic on-device generation cadence
  * Instant cancellation handling (<1s) upon coroutine cancellation
  * Safe memory management with `unloadModel()` and `onTrimMemory` integration
- Wired Dependency Injection & Engine Router:
  * `AppContainer`: Added `modelManager`, `liteRtLocalEngine`, and wired `localEngine` into `engineRouter`
  * `CodeMateApp`: Hooked `onTrimMemory` lifecycle event to `liteRtLocalEngine.onTrimMemory(level)`
  * `EngineRouter`: Enhanced `AUTO` strategy with seamless cloud-to-local fallback
- Implemented Model Manager UI:
  * `fragment_model_manager.xml`: Hardware capability card with RAM/storage chips, Wi-Fi switch, SAF import button, active model quick-test card, and catalog RecyclerView
  * `item_model_catalog.xml`: Tier badge, model description, specs row, status indicators, download progress bar, and action buttons
  * `ModelManagerViewModel`: Manages catalog state, enqueues WorkManager downloads, observes progress, handles deletion and quick inference test
  * `ModelManagerAdapter`: ListAdapter binding items dynamically with Material 3 theme colors
  * `ModelManagerFragment`: Full screen interaction with SAF picker and confirmation dialogs
- Integrated Official LiteRT-LM Native Runtime (`com.google.ai.edge.litertlm:litertlm-android:0.17.1`):
  * Packaged `liblitertlm_jni.so` native runtime into debug APK
  * Direct on-device inference via `Engine` and `Conversation` using downloaded `.litertlm` model files
  * Seamless fallback to `OfflineTutorKnowledgeBase` if native library is unavailable
- Implemented Offline Programming Tutor Knowledge Base (`OfflineTutorKnowledgeBase.kt`):
  * Rich DSA explanations and syntax-tagged code in Java, Python, C++, and Kotlin
  * Support for BFS, DFS, Dijkstra, TopoSort, MST (Kruskal/Prim), Trie, BST, Sorting (Quick/Merge/Bubble/Insertion/Selection), Lists, Stacks, Queues, HashMaps, Heaps, DP, OOP, Interfaces vs Abstract classes, and Big-O complexity
  * Core fundamentals: Primitive & modifier data types, sum of two numbers, prime check, factorials, palindromes, in-place reverse, bitwise even/odd, pointers/references, loops
  * Clean, language-idiomatic syntax across all outputs (no generic Java stubs for C++/Python)
- Automated Unit Tests (119/119 passed, 0 failures, 0 skipped):
  * `OfflineTutorKnowledgeBaseTest`: 7 tests
  * `ModelCatalogTest`: 3 tests
  * `DeviceCapabilityCheckerTest`: 5 tests
  * `ModelManagerTest`: 6 tests
  * `LiteRtLocalEngineTest`: 7 tests
  * `EngineRouterTest`: 6 tests
  * `ModelManagerViewModelTest`: 5 tests
  * Previous test suites: 80 tests (Chat, Auth, Settings, Database, Repositories, Curriculum, Progress, UiText)
- Verification gates:
  * G1 Build: assembleDebug (BUILD SUCCESSFUL)
  * G2 Lint: lintDebug (BUILD SUCCESSFUL, 0 errors)
  * G3 Unit tests: testDebugUnitTest (BUILD SUCCESSFUL, 119 passed)
  * G4 Instrumented: Ready for physical device execution over USB
  * G5 Physical phone smoke: Manual checklist provided (debug APK built)
  * G6 Monkey: NOT RUN on personal device per workflow rules
  * G7 Screenshots: Visual checks via physical phone manual inspection
In progress: None
Next:
- Awaiting human physical phone manual verification & acceptance of Phase 5
- Tag `phase-05-done` upon acceptance
- Start Phase 6: Core Features — Code Explanation, Debugger, Learning Mode (Socratic)
How to run (Physical-Device-First):
- G1 Build: `.\gradlew.bat --console=plain assembleDebug`
- G2 Lint: `.\gradlew.bat --console=plain lintDebug`
- G3 Unit tests: `.\gradlew.bat --console=plain testDebugUnitTest`
- Install to physical phone: `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk`
Known issues: None
Needs human: Manual checklist verification on physical device and acceptance of Phase 5
Key versions:
- AGP: 9.2.1
- Gradle: 9.4.1
- Kotlin: 2.2.10 (AGP built-in)
- KSP: 2.3.6
- Room: 2.7.0
- WorkManager: 2.10.0
- Navigation: 2.8.9
- OkHttp: 4.12.0
