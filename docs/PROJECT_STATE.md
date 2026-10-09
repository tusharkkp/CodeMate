# PROJECT STATE
Updated: 2026-10-10 01:42 IST   Branch: phase/04-ai-gemini-chat   Last commit: a56bdfe feat(ai): implement AI engine abstraction, Gemini cloud streaming, and Chat UI
Current phase: 4 — AI Abstraction + Gemini Cloud Chat   Status: pending-human (automated gates G1-G3 green; ready for physical phone verification)
Done:
- Tagged `phase-03-done` on phase 3 acceptance
- Branched `phase/04-ai-gemini-chat`
- Implemented AI Abstraction & Configuration:
  * `AiConfig`: Centralized model parameters (`gemini-2.5-flash`), HTTP timeouts (15s connect, 30s read), turn budget limits (10 cloud / 4 local), default temperature (0.7)
  * `LlmModels`: Typed contracts for `LlmRequest`, `LlmChunk` (Delta, Done, Failure), `TokenUsage`, `ChatTurn`, `EngineStatus`
  * `LlmError`: Comprehensive typed error mapping (`NoNetwork`, `BadKey`, `RateLimited`, `Server`, `Timeout`, `ModelMissing`, `SafetyBlocked`, `Unknown`) with localized user strings
  * `LlmEngine`: Universal interface for language models
  * `FakeLlmEngine`: Deterministic fake supporting Instant, Streaming, and Error modes for testing and preview
  * `EngineRouter`: Intelligent engine resolver for Cloud, Local, and Auto routing strategies
- Implemented Google Gemini Cloud Engine:
  * `GeminiModels`: Kotlinx serialization data classes for Gemini v1beta streamGenerateContent API
  * `GeminiCloudEngine`: Native OkHttp SSE stream parser reading `data:` events
  * Cancellation AC: Coroutine cancellation triggers `call.cancel()` aborting the socket in < 1 second
  * Safety & error handling: Filters safety blocks (`finishReason: SAFETY`), maps 401/403 to `BadKey`, 429 to `RateLimited`, 500s to `Server`
- Implemented Prompt Engineering:
  * `PromptTemplates`: Expert tutor system instructions with language/level adaptation and off-topic guardrails; compact mode for offline use
  * `PromptBuilder`: Context window trimmer enforcing token and turn limits (10 cloud, 4 local)
- Implemented Markdown & Code Block Rendering:
  * `MarkdownRenderer`: Markwon instance with `TablePlugin` for rendering markdown and tables in Android TextViews
- Implemented Chat Screen UI:
  * `fragment_chat.xml`: Material 3 top chips bar (Language, Level, Engine), transient error banner, `RecyclerView`, state views (`LoadingStateView`, `EmptyStateView`, `ErrorStateView`), message input with `Send` and `Stop` buttons
  * `item_chat_message_user.xml`: User message bubble with primary color styling
  * `item_chat_message_assistant.xml`: Assistant message bubble with engine badges (`☁ Gemini`), copy button, regenerate button, and typing indicator
  * `ChatAdapter`: DiffUtil ListAdapter handling User and Assistant view types
  * `ChatFragment`: Reactive UI binding with `repeatOnLifecycle(STARTED)`, interactive dialogs for language/level/engine selection, clipboard copying
- Implemented Chat ViewModel & Room Persistence:
  * `ChatUiState`: Immutable state tracking messages, language, level, engine, streaming state, error messages
  * `ChatViewModel`: Coordinates `ChatRepository` message storage, `EngineRouter`, transient streaming display (using id -1L), and graceful cancellation saving partial responses exactly once
  * `AppContainer`: Wired `GeminiCloudEngine`, `FakeLlmEngine`, and `EngineRouter` singletons
- Automated Unit Tests (77/77 passed):
  * `FakeLlmEngineTest`: 5 tests
  * `GeminiCloudEngineTest`: 6 tests with `MockWebServer` (SSE streaming, 401 BadKey, 429 RateLimited, 503 Server, Safety block, blank key)
  * `PromptBuilderTest`: 5 tests (cloud history trim, local history trim, language/level interpolation, explain/debug mode)
  * `ChatViewModelTest`: 5 tests (load conversation, send & stream, stop & partial save, error handling, preferences)
  * Previous test suites: 56 tests (Auth, Settings, Repositories, Database, Curriculum, Progress, UiText)
- Verification gates:
  * G1 Build: assembleDebug (BUILD SUCCESSFUL)
  * G2 Lint: lintDebug (BUILD SUCCESSFUL, 0 errors)
  * G3 Unit tests: testDebugUnitTest (BUILD SUCCESSFUL, 77 passed)
  * G4 Instrumented: Ready for physical device execution over USB
  * G5 Physical phone smoke: Manual checklist provided (debug APK built)
  * G6 Monkey: NOT RUN on personal device per workflow rules
  * G7 Screenshots: Visual checks via physical phone manual inspection
In progress: None
Next:
- Awaiting human physical phone manual verification & acceptance of Phase 4
- Tag `phase-04-done` upon acceptance
- Start Phase 5: On-Device LLM & Model Manager (LiteRT-LM)
How to run (Physical-Device-First):
- G1 Build: `.\gradlew.bat --console=plain assembleDebug`
- G2 Lint: `.\gradlew.bat --console=plain lintDebug`
- G3 Unit tests: `.\gradlew.bat --console=plain testDebugUnitTest`
- Install to physical phone: `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk`
Known issues: None
Needs human: Manual checklist verification on physical device and acceptance of Phase 4
Key versions:
- AGP: 9.2.1
- Kotlin: built-in (KGP 2.2.10 runtime)
- KSP: 2.3.6
- Room: 2.7.0
- Navigation: 2.8.5
- Material: 1.12.0
- Gemini Model ID: gemini-2.5-flash
- LiteRT-LM: to be configured in Phase 5
