# PROJECT STATE
Updated: 2026-10-10 00:05 IST   Branch: phase/02-database-datastore   Last commit: 6d11492 feat(data): implement Room entities, DAOs, DataStore, and repositories
Current phase: 2 — Database & Local Persistence   Status: pending-human (automated gates G1-G3 green; ready for physical phone verification)
Done:
- Tagged `phase-01-done` on phase 1 acceptance
- Branched `phase/02-database-datastore`
- Implemented Room database architecture (CodeMateDatabase v1, exportSchema = true):
  * 9 entities: UserEntity, ConversationEntity, MessageEntity, MessageFtsEntity (FTS4 full-text search), QuizAttemptEntity, QuizAnswerEntity, TopicProgressEntity, DailyActivityEntity, SavedSnippetEntity
  * 7 DAOs: UserDao, ConversationDao, MessageDao, QuizDao, TopicProgressDao, DailyActivityDao, SavedSnippetDao
  * Type converters for domain enums: TutorMode, MessageRole, LearnerLevel, EngineType, Difficulty
  * Verified schema export at `app/schemas/com.mit.tushar_kaldate.data.local.CodeMateDatabase/1.json`
- Implemented DataStore stores:
  * SessionStore (userId, guestMode, email, displayName, loginTimestamp)
  * SettingsStore (theme: SYSTEM/LIGHT/DARK, defaultMode, defaultEngine, hapticFeedback, streamResponse, codeFontSizeSp)
- Seed curriculum & loader:
  * Bundled curriculum in assets (`app/src/main/assets/curriculum.json`) with topics for Python, Java, C++, DSA, and Web Basics
  * CurriculumLoader for asset loading and parsing into structured models
- Security & Repositories:
  * PasswordHasher: Salted PBKDF2WithHmacSHA256 (10,000 iterations, 32-byte salt, 256-bit hash)
  * AuthRepository (register, login, guest login, session observation, logout)
  * ChatRepository (conversation CRUD, message persistence, FTS search, snippet bookmarks)
  * QuizRepository (attempt recording, per-question answer tracking, history queries)
  * ProgressRepository (mastery calculation with decay formula, streak tracking across midnight)
  * SearchRepository (FTS search across message history with snippet highlighting)
  * AppContainer updated with singletons for Database, DAOs, DataStores, and Repositories
- Unit test suite:
  * PasswordHasherTest (5 tests: hash format, verification, invalid passwords, unique salts)
  * ProgressCalculationTest (6 tests: mastery calculation, retention decay, streak increments)
  * CurriculumLoaderTest (2 tests: valid json parsing, integrity check)
  * AuthRepositoryTest (9 tests: register, duplicate email, login success/failure, guest mode, logout)
  * SearchRepositoryTest (2 tests: empty query handling, result mapping)
  * UiTextTest (5 tests) & SanityUnitTest (3 tests)
  * Total unit tests: 32/32 passing (0 failures, 0 skipped)
- Instrumented test suite:
  * RoomDatabaseTest (in-memory Room test covering User CRUD, cascade deletion, FTS4 search, quiz attempts, mastery updates)
- Verification gates:
  * G1 Build: assembleDebug (BUILD SUCCESSFUL)
  * G2 Lint: lintDebug (BUILD SUCCESSFUL, 0 errors)
  * G3 Unit tests: testDebugUnitTest (BUILD SUCCESSFUL, 32 passed)
  * G4 Instrumented: RoomDatabaseTest ready for execution when physical device attached via USB
  * G5 Physical phone smoke: Manual checklist provided (app-debug.apk built)
  * G6 Monkey: NOT RUN on personal device per workflow rules
  * G7 Screenshots: Visual checks via physical phone manual inspection
In progress: None
Next:
- Awaiting human physical phone manual verification & acceptance of Phase 2
- Tag `phase-02-done` upon acceptance
- Start Phase 3: Auth, settings, session & offline sync (branch `phase/03-auth-settings-sync`)
How to run (Physical-Device-First):
- G1 Build: `.\gradlew.bat --console=plain assembleDebug`
- G2 Lint: `.\gradlew.bat --console=plain lintDebug`
- G3 Unit tests: `.\gradlew.bat --console=plain testDebugUnitTest`
- Install to physical phone: `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk`
Known issues: None
Needs human: Manual checklist verification on physical device and acceptance of Phase 2
Key versions:
- AGP: 9.2.1
- Kotlin: built-in (KGP 2.2.10 runtime)
- KSP: 2.3.6
- Room: 2.7.0 (upgraded from 2.6.1 for KSP 2 compatibility)
- Navigation: 2.8.5
- Material: 1.12.0
- Gemini Model ID: gemini-2.5-flash (to be configured in AiConfig)
- LiteRT-LM: to be configured in Phase 5

