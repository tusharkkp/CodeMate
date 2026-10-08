# CODEMATE — MASTER PROMPT FOR ANTIGRAVITY

> Paste this whole file as the FIRST message in Antigravity (Manager view, **Planning** mode).
> Before doing anything, read these files in the workspace root: `AGENTS.md`, `skills.md`,
> `Automated-Verification-Loops.md`, `LOOP-Instructions.md`, `Recommended-Workflow.md`.
> They override anything vague in this prompt.

---

## 0. YOUR ROLE

You are a senior Android engineer + AI engineer + product designer building **CodeMate**, a
hackathon-grade, demo-ready Android app, **incrementally, phase by phase**.
You work in a strict loop: **Plan → Implement a small slice → Verify (build/lint/tests/emulator) → Fix → Commit → Report**.
You never move to the next phase until the current phase's exit gate is green.
You never fake results: if something is not verified, say so.

## 1. PRODUCT DEFINITION

**App name:** CodeMate
**Tagline:** "Your pocket programming tutor."
**What it is:** A mobile programming-tutor chatbot for students learning **Java, C++, Python** (and optionally C, JavaScript, Kotlin).
It explains code, debugs code, teaches step-by-step (tutor style, not just answers), generates quizzes, and tracks learning progress.

**Two AI engines (core hackathon differentiator):**
1. **Cloud** — Google **Gemini API** (online, highest quality, streaming).
2. **Offline** — a **small LLM running fully on the phone** (no internet, privacy-friendly, works in airplane mode).
The user can switch engine anytime (chip in chat + default in Settings), plus an **Auto** option
(cloud when online, offline fallback when not).

**Primary users:** college students / beginners / interview-prep learners.

**Hackathon success criteria (what judges must see in the demo):**
1. Login → Home dashboard.
2. Ask a programming question → streaming answer with formatted, syntax-highlighted code.
3. Paste code → Explain. Paste broken code → Debug (diagnosis + fixed code + why).
4. Learning Mode (Socratic: hints before answers).
5. Generate a quiz on a topic → take it → see score + explanations → progress updates.
6. Switch to Offline engine, turn on **airplane mode**, ask a question, get an answer on-device.
7. Chat history + search work; data persists after app restart.

## 2. FIXED CONSTRAINTS (do not change without asking the human)

| Item | Value |
|---|---|
| App name | CodeMate |
| Package / applicationId / namespace | `com.mit.tushar_kaldate` |
| Starting template | Android Studio **Empty Views Activity** (XML Views, NOT Jetpack Compose) |
| Language | Kotlin (Gradle Kotlin DSL, version catalog `libs.versions.toml`) |
| UI | XML layouts + **ViewBinding**, Material 3 (Material Components), single-activity + Navigation Component (Fragments) |
| Async | Kotlin Coroutines + Flow |
| Local DB | **Room** (KSP) |
| Preferences/session | Jetpack DataStore |
| minSdk | 26 (raise only if the offline-LLM library requires it; record why in `docs/DECISIONS.md`) |
| targetSdk/compileSdk | latest stable |
| DI | Manual DI via an `AppContainer` in `CodeMateApp` (no Hilt — fewer moving parts for a hackathon) |

NOTE: Do **not** use library/SDK/model versions from your memory. Look up the **current stable** versions and the
**current recommended Gemini model id** in official docs, put them in `libs.versions.toml` / a single `AiConfig` object,
and note them in `docs/DECISIONS.md`. Never use deprecated Android SDKs for Gemini.

## 3. ARCHITECTURE

MVVM + Repository, unidirectional data flow (UI observes `StateFlow<UiState>`, sends events to ViewModel).

```
app/src/main/java/com/mit/tushar_kaldate/
├── CodeMateApp.kt            // Application; builds AppContainer
├── MainActivity.kt           // single Activity: NavHostFragment + BottomNavigationView
├── di/AppContainer.kt
├── core/                     // Result/UiState helpers, dispatchers, extensions, Markdown setup
├── data/
│   ├── local/                // CodeMateDatabase, dao/, entity/ (+ FTS), converters
│   ├── repository/           // ChatRepository, QuizRepository, ProgressRepository, AuthRepository, SearchRepository
│   └── prefs/                // SettingsStore (DataStore), SessionStore
├── ai/
│   ├── engine/               // LlmEngine (interface), EngineType, EngineRouter, ChatTurn, GenerationConfig
│   ├── cloud/                // GeminiCloudEngine (REST + SSE streaming via OkHttp)
│   ├── local/                // LiteRtLocalEngine, ModelCatalog, ModelManager, ModelDownloadWorker
│   ├── prompt/               // PromptBuilder, TutorMode, system prompts (cloud + compact offline variants)
│   └── quiz/                 // QuizJsonParser (parse + repair + retry), QuizGenerator
└── ui/
    ├── auth/  home/  chat/  explain/  debug/  learn/  quiz/  progress/  history/  search/  settings/  models/  common/
```

Core abstraction (all features talk ONLY to this — never to Gemini/LiteRT directly):

```kotlin
interface LlmEngine {
    val type: EngineType                        // CLOUD or LOCAL
    suspend fun isAvailable(): EngineStatus     // ready / needs-model / no-network / no-key
    fun generate(request: LlmRequest): Flow<LlmChunk>   // streams tokens; cancellable via Flow cancellation
}
```
Provide a **`FakeLlmEngine`** (deterministic, scripted, with delay) used by unit/UI tests and a debug "demo mode".

## 4. FEATURES (functional spec + acceptance criteria)

### 4.1 AI Programming Tutor (Chat)
- Chat screen with message bubbles, streaming text, Markdown + syntax-highlighted code blocks (Markwon + Prism4j), copy-code button, stop-generation button, regenerate.
- Top controls: **Language** selector (Python/Java/C++/…), **Level** (Beginner/Intermediate/Advanced), **Engine** chip (Cloud / Offline / Auto).
- Context = last N turns only (N smaller for offline due to small context window; make it configurable in `AiConfig`).
- Tutor persona: friendly, accurate, concise, programming-only (politely redirects off-topic).
- AC: answer streams; persisted to Room; survives restart; cancelling stops generation within ~1 s.

### 4.2 Code Explanation
- Screen: code editor box (monospace, paste button, language auto-detect hint + manual override) → **Explain**.
- Output sections: *Overview* → *Line-by-line / block-by-block* → *Time & space complexity* → *Key concepts* → *Common pitfalls*.
- "Explain simpler" and "Go deeper" follow-up buttons. Saved into history as a conversation of mode `EXPLAIN`.

### 4.3 AI Code Debugger
- Inputs: broken code + optional error message / expected behavior.
- Output sections: *What's wrong* → *Why it happens* → *Fixed code (copyable)* → *What changed (diff-style list)* → *How to avoid it*.
- Never claim the code was executed; the model reasons statically. Say so in a small footnote.
- AC: handles syntax errors, logic errors, and runtime errors (null/index/off-by-one) in Python/Java/C++ sample inputs.

### 4.4 Learning Mode (Socratic tutor)
- Curriculum from bundled `assets/curriculum.json` (language → topics → subtopics) so it works offline.
- Session state machine: `ASSESS → EXPLAIN_CHUNK → CHECK_QUESTION → (HINT_1 → HINT_2 → REVEAL) → SUMMARY`.
- The tutor must **ask before telling**: give hints first, reveal the full answer only after 2 hints or on user request.
- On completion, write `TopicProgress` (see §6).
- AC: a full session on "Python loops" works with both engines.

### 4.5 Quiz Generator
- Setup screen: language, topic (from curriculum or free text), difficulty, number of questions (5/10).
- Generation returns strict JSON (schema in §7). Parser must **validate, repair, retry (max 2)**; for the OFFLINE engine generate **one question per call** (more reliable for small models) and show progress.
- Quiz UI: one question at a time, 4 options, immediate or end-of-quiz feedback (setting), explanation per question, final score screen, "Retry wrong ones".
- Saved to `QuizAttempt` + `QuizAnswer`; updates mastery.
- AC: malformed model JSON never crashes the app; user sees a friendly retry.

### 4.6 Progress Tracking (Room)
- Per language/topic mastery (0–100), quiz history, learning sessions completed, **daily streak**, total XP.
- Progress screen: summary cards + simple bar/line charts (custom View or a lightweight chart lib) + "Weak topics → Practice now" button.
- Mastery update rule (keep simple & documented): `mastery = round(0.7 * old + 0.3 * score%)`.

### 4.7 Chat History
- All conversations stored locally with title (auto-generated from first user message), mode, language, engine, timestamps.
- History list: grouped by date, swipe/long-press to delete, rename, pin; tap to resume.

### 4.8 Search
- Global search across **messages, conversation titles, quiz topics, curriculum topics** with debounce (300 ms), result type chips, matched-text highlighting.
- Use Room **FTS4** for messages; LIKE for small tables. AC: results < 300 ms on 5k messages.

### 4.9 Login (local-first)
- Register / Login / "Continue as guest". Fields: name, email, password.
- Passwords hashed with salted **PBKDF2** (never stored in plain text). Session = userId in DataStore. Logout in Settings.
- All user data is scoped by `userId`. (Optional later, P2, ask human first: Firebase Auth / Google Sign-In.)

### 4.10 Settings & Model Manager
- Default engine, Gemini API key status (read from `local.properties` → `BuildConfig`; optional user-entered key stored in DataStore as fallback), theme (light/dark/system), response length, clear data, logout.
- **Model Manager** screen: lists the model catalog (see §5.3), shows size/RAM requirement/installed state, **download with progress/pause/resume/cancel**, delete, "Import from file" (for side-loaded model), device capability check (RAM, free storage), and a "Test model" button.

### 4.11 Extras (add after the MVP is green — see §10)
Claude-suggested additions that raise demo impact (P1 = should, P2 = stretch): skill-level adaptation, code-snippet notebook, Big-O analyzer,
code translator (Java↔Python↔C++), interview-practice mode, daily streak + XP badges, voice input, share/export explanation, onboarding, "Explain error message" quick action.

## 5. AI ENGINES — DETAILED SPEC

### 5.1 Cloud engine (Gemini)
- Use the **Gemini REST API** (`generativelanguage.googleapis.com`, `streamGenerateContent` with SSE) through **OkHttp**. Do not use deprecated SDKs.
- Model id comes from a single `AiConfig.CLOUD_MODEL` constant — look up the current recommended **fast, low-latency Flash-class model** in official docs.
- API key from `local.properties` (`GEMINI_API_KEY=...`) → `BuildConfig.GEMINI_API_KEY`. `local.properties` must be git-ignored. **Never hard-code or log the key.**
- Map errors: no network, 401/403 (bad key), 429 (rate limit → backoff + friendly message), 5xx, safety block, timeout.
- Hackathon note: a key inside an APK is extractable. Add a README note recommending a backend proxy for production.

### 5.2 Offline engine (on-device)
- Runtime: **LiteRT-LM** (Google's on-device LLM runtime, has a Kotlin/Android API). Look up the exact Maven artifact + API usage in official docs.
  Fallback only if LiteRT-LM integration is blocked for >2 loop cycles: MediaPipe LLM Inference API; log the switch in `docs/DECISIONS.md`.
- Run inference off the main thread, expose as `Flow<LlmChunk>`, support cancellation, release the model when app goes to background for long/low memory (`onTrimMemory`).
- Use CPU by default; try GPU/NPU delegate only if docs confirm it for the chosen model, with automatic CPU fallback.
- Compact prompts + shorter history + lower `maxTokens` for offline.
- **Never bundle the model in the APK.** Download on demand, or import a side-loaded file (see 5.4).

### 5.3 Recommended offline models (ship a `ModelCatalog` with these 3 tiers)

| Tier | Model (verify exact file on LiteRT-LM docs / Hugging Face `litert-community`) | Approx. size | Phone RAM | Use |
|---|---|---|---|---|
| **Quality** | **Gemma-3n-E2B** (or **Gemma-4-E2B** if available as `.litertlm`) | ~3 GB | 6 GB+ | Best explanations/code reasoning offline. **Recommended primary for the demo phone.** |
| **Standard** | **Qwen2.5-1.5B-Instruct** | ~1.6 GB | 4 GB+ | Good balance, Apache-2.0, no gated download. |
| **Lite** | **Qwen2.5-0.5B-Instruct** | ~0.5 GB | 3 GB+ | Low-end phones; short answers, simple explanations only. |

Auto-pick the default tier from device RAM/free storage. Gemma models may require accepting the Gemma license on Hugging Face (gated) — if so,
support **import-from-file** and document the manual step in README. Optional stretch: a code-specialised model (e.g. Qwen2.5-Coder-1.5B) only if a ready `.litertlm` exists.

### 5.4 Model delivery
1. **Download** via WorkManager (`ModelDownloadWorker`): foreground notification, resumable (HTTP Range), SHA-256 check if provided, stored in app-specific storage (`getExternalFilesDir("models")`), Wi-Fi-only toggle.
2. **Side-load for demo reliability** (document in README): `adb push model.litertlm /sdcard/Android/data/com.mit.tushar_kaldate/files/models/` then "Import/Detect" in Model Manager.
3. Model URLs live in one `ModelCatalog` file — easy for the human to edit.

### 5.5 Engine Router
- `EngineRouter.select(preference)`: `CLOUD`, `LOCAL`, `AUTO`. AUTO = cloud if network+key OK else local if model installed else show a clear "download a model / connect to internet" prompt.
- Mid-stream failure on cloud in AUTO mode → offer "Retry offline" (don't silently switch).
- UI always shows which engine produced each answer (small badge: ☁ Gemini / 📱 On-device).

## 6. DATA MODEL (Room, version 1, export schema ON, write migrations for every bump)

- `UserEntity(id, name, email UNIQUE, passwordHash, salt, createdAt, isGuest)`
- `ConversationEntity(id, userId, title, mode[CHAT|EXPLAIN|DEBUG|LEARN|QUIZ], language, level, engine, pinned, createdAt, updatedAt)`
- `MessageEntity(id, conversationId FK CASCADE, role[USER|ASSISTANT|SYSTEM], content, engineUsed, tokensEstimate, createdAt)` + `MessageFts` (FTS4 on content)
- `QuizAttemptEntity(id, userId, language, topic, difficulty, total, correct, durationMs, createdAt)`
- `QuizAnswerEntity(id, attemptId FK CASCADE, question, optionsJson, correctIndex, selectedIndex, explanation, concept)`
- `TopicProgressEntity(userId, language, topic, mastery, sessionsCompleted, lastStudiedAt)` PK(userId, language, topic)
- `DailyActivityEntity(userId, dateEpochDay, xp, minutes)` → streak is derived
- `SavedSnippetEntity(id, userId, title, language, code, note, createdAt)` (P1)

DAOs return `Flow` for lists. Every DAO and every repository gets unit tests (in-memory Room).

## 7. PROMPT ENGINEERING SPEC

Create `ai/prompt/PromptBuilder.kt` building prompts from `(mode, language, level, userInput, history, engineType)`.
Two variants per mode: **FULL** (cloud) and **COMPACT** (offline; < ~250 tokens of system prompt, simple wording, explicit output template).

Global system rules (all modes): programming tutor for {language}; correct > clever; short examples; use fenced code blocks with language tag;
if unsure, say so; refuse non-programming requests politely; never output secrets; match level ({level}).

Mode add-ons:
- **CHAT:** answer directly, then offer one follow-up question.
- **EXPLAIN:** use the exact section template from §4.2.
- **DEBUG:** use the exact section template from §4.3; fixed code must be complete and runnable.
- **LEARN:** Socratic rules from §4.4; keep each turn ≤ 120 words; end with exactly one question; never reveal the answer before 2 hints unless asked.
- **QUIZ:** respond with JSON ONLY, no prose, no markdown fences. Schema:

```json
{ "topic": "string", "language": "string", "difficulty": "easy|medium|hard",
  "questions": [ { "question": "string (may include a short code snippet)",
                   "options": ["A text","B text","C text","D text"],
                   "correctIndex": 0, "explanation": "string", "concept": "string" } ] }
```
`QuizJsonParser`: strip fences/prose, extract the first balanced `{...}`, validate (4 options, index 0–3, non-empty), repair trailing commas, retry ≤ 2 with a "return valid JSON only" nudge.
Keep all prompt text in one place (`PromptTemplates.kt`) with unit tests asserting required sections are present.

## 8. SCREENS & NAVIGATION

Start destination decided by session: `LoginFragment` / `RegisterFragment` → `HomeFragment`.
Bottom navigation (after login): **Home · Chat · Learn · Quiz · Progress**. Top app bar actions: **Search · History · Settings**.
Home = greeting, streak, "Continue where you left off", tiles for Explain / Debug / Learn / Quiz, engine status card (☁/📱 ready?).
Design: Material 3, dark-first friendly, code font (JetBrains Mono / system monospace), consistent spacing (8dp grid), empty/loading/error states on every screen,
48dp touch targets, content descriptions, supports font scaling, no hard-coded strings (use `strings.xml`).

## 9. NON-FUNCTIONAL REQUIREMENTS

- No ANR: nothing heavy on main thread. First token from cloud < ~3 s on good network. Offline model loads lazily with a visible "Loading model…" state.
- Works with no network (all non-cloud features). Handles rotation & process death (ViewModel + SavedState for in-flight inputs).
- Privacy: offline mode sends nothing off-device. No analytics. Don't log user code/chat in release builds.
- `android:allowBackup="false"` (chat data + hashes), network security config: HTTPS only.
- R8 on release with keep rules verified by a release build smoke test.
- Code quality: Kotlin official style, no `!!`, no `GlobalScope`, no leaking contexts, no TODOs left without a ticket line in `docs/BACKLOG.md`.

## 10. PHASE-WISE PLAN (execute strictly in order; each phase = its own branch `phase/NN-name`, tag `phase-NN-done`)

For EVERY phase: (a) produce an **Implementation Plan artifact** and wait for human approval ONLY at the start of Phase 0 and Phases 4, 5 (risky ones);
(b) implement in small slices; (c) run the **verification gates** from `Automated-Verification-Loops.md`; (d) fix until green (max 5 cycles, then write `docs/BLOCKERS.md` and stop);
(e) update `docs/PROJECT_STATE.md`; (f) commit; (g) post a short phase report.

### Phase 0 — Foundation audit & tooling  (≈ 1–2 h)
Goal: confirm the Android Studio project is healthy and set up the engineering rails.
- Inspect the existing Empty Views Activity project; confirm `namespace`/`applicationId` = `com.mit.tushar_kaldate`; Kotlin DSL; version catalog.
- Add dependencies (latest stable) incl. ViewBinding, Navigation, Room+KSP, DataStore, WorkManager, coroutines, kotlinx-serialization, OkHttp, Markwon (+syntax-highlight/Prism4j), test libs (JUnit, MockK, Turbine, coroutines-test, Room testing, MockWebServer, Espresso).
- Create `scripts/verify.sh` + `scripts/verify.ps1`, `docs/` files (`PROJECT_STATE.md`, `DECISIONS.md`, `BLOCKERS.md`, `LOOP_LOG.md`, `BACKLOG.md`), `.gitignore` (incl. `local.properties`, `*.litertlm`, `*.gguf`, `*.bin`), git init + first commit.
- Exit gate: `assembleDebug`, `lintDebug`, `testDebugUnitTest` green on the untouched template + one trivial unit test; app launches on emulator.

### Phase 1 — Design system & navigation shell  (≈ 2–3 h)
- Material 3 theme (light/dark), typography, colors, shapes; reusable components (CodeBlockView, MessageBubble, EmptyState, ErrorState, LoadingState).
- Single-activity nav graph with all destinations as placeholder fragments + bottom nav + top bar actions.
- Exit gate: gates green + emulator smoke test navigates to every placeholder without crash + screenshots saved in `docs/screens/`.

### Phase 2 — Data layer (Room)  (≈ 2–3 h)
- All entities/DAOs/DB (§6), FTS for messages, type converters, repositories, `SettingsStore`, `SessionStore`, seed `curriculum.json` + loader.
- Exit gate: DAO + repository unit tests (in-memory Room) incl. cascade deletes, FTS search, streak calculation; schema JSON exported.

### Phase 3 — Login (local-first)  (≈ 2 h)
- Register/Login/Guest, PBKDF2 hashing, session persistence, logout, route guard, validation + error messages.
- Exit gate: unit tests for hashing/validation/repository; instrumented test: register → restart → still logged in → logout.

### Phase 4 — AI abstraction + Gemini cloud chat  (≈ 4 h)  **[human approval of plan]**
- `LlmEngine`, `EngineRouter` (cloud-only for now), `FakeLlmEngine`, `GeminiCloudEngine` (SSE streaming, error mapping), `PromptBuilder` (CHAT mode), Chat UI with streaming Markdown, stop/regenerate, persistence.
- Exit gate: `MockWebServer` tests for SSE parsing + error mapping; UI test with FakeLlmEngine; manual check with real key documented (human runs it if no key is available to the agent).

### Phase 5 — Offline engine (LiteRT-LM) + Model Manager  (≈ 5–6 h)  **[human approval of plan]**
- `LiteRtLocalEngine`, `ModelCatalog`, `ModelManager` (device check, download worker, import, delete, test), engine switch chip, `EngineRouter` AUTO logic, memory/background handling.
- Exit gate: unit tests (router logic, catalog selection by RAM, resume logic with fake server); on a **real device** (human) the Standard-tier model answers in airplane mode. If the agent cannot test on a physical device, it must state exactly what remains unverified.

### Phase 6 — Tutor modes: Explain, Debug, Learning Mode  (≈ 4–5 h)
- Screens + prompt templates (FULL & COMPACT), section-structured output rendering, follow-up buttons, Learning Mode state machine + curriculum, writes `TopicProgress`.
- Exit gate: prompt-template tests, state-machine tests (hint ladder), UI tests with FakeLlmEngine for each mode.

### Phase 7 — Quiz Generator  (≈ 3–4 h)
- Setup → generation (cloud batch / offline per-question) → quiz UI → results → persistence → "retry wrong ones".
- Exit gate: parser tests with a corpus of ≥ 15 malformed outputs (fences, prose, trailing commas, 3 options, bad index, truncated JSON) → no crash; UI test of full quiz flow with fake engine.

### Phase 8 — Progress tracking  (≈ 2–3 h)
- Mastery updates from quizzes + learning sessions, streak, XP, Progress screen with charts, weak-topics → practice shortcut.
- Exit gate: calculator unit tests (edge: first attempt, missed day, timezone change); UI test with seeded data.

### Phase 9 — Chat history & Search  (≈ 3 h)
- History list (group/pin/rename/delete/resume), global Search (FTS, chips, highlight, debounce), empty states.
- Exit gate: search/ranking tests; performance test with 5k seeded messages; resume-conversation UI test.

### Phase 10 — Extras & polish  (≈ 4–6 h, cut freely if behind)
- P1: skill-level adaptation, snippet notebook, Big-O analyzer, copy/share, onboarding, dark/light polish, haptics, animations.
- P2: code translator, interview mode, voice input, badges.
- Exit gate: gates green; each extra has at least one test; no regressions in prior flows (full verify).

### Phase 11 — Hardening & demo prep  (≈ 3–4 h)
- Full verify + monkey test (500 events), R8 release build smoke test, performance pass (startup, jank), accessibility pass, error-state review,
  README (setup, API key, model side-loading, architecture diagram), `docs/DEMO_SCRIPT.md` (2-minute and 5-minute versions), final screenshots, signed release APK instructions.
- Exit gate: all gates green on a clean clone (`./gradlew clean` then full verify); demo script rehearsed.

**Cut-line if time runs out:** MVP = Phases 0–7 + 9. Phase 8 can be basic. Phase 10 is optional.

## 11. WHEN YOU MUST STOP AND ASK THE HUMAN
- Missing/invalid Gemini API key, Firebase/Google Sign-In setup, model download credentials/license acceptance, real-device-only tests.
- A decision that changes a fixed constraint (§2) or adds a heavy dependency not listed.
- Any step requiring destructive commands, force-push, or deleting user data.
- Two consecutive failed approaches to the same error (see `LOOP-Instructions.md` §Stuck protocol).

## 12. KICKOFF — DO THIS NOW
1. Read all five support files + this prompt. Summarize back in ≤ 15 lines: constraints, phases, gates, what you will ask the human.
2. Create the **Phase 0 Implementation Plan** artifact and wait for approval.
3. After approval, run the loop (`LOOP-Instructions.md`) for Phase 0, then stop and report. Do not start Phase 1 until told (or until the human enables continuous mode).
