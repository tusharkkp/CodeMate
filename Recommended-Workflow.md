# Recommended Workflow — building CodeMate with Antigravity

## 1. One-time setup (≈ 45 min — do this BEFORE the hackathon clock starts if you can)

1. **Android Studio:** New Project → **Empty Views Activity** → Name `CodeMate`, Package `com.mit.tushar_kaldate`, Language Kotlin, Min SDK 26, Build config: Kotlin DSL.
   Let Gradle sync finish and run the template once on an emulator.
2. **Emulator:** create an AVD (Pixel, API 34+, x86_64 image, ≥ 4 GB RAM). Also keep a **real Android phone** with USB debugging for the offline-model demo (offline LLMs are slow/unreliable on emulators).
3. **Gemini key:** get an API key from Google AI Studio → add to `local.properties` in the project root: `GEMINI_API_KEY=your_key` (never commit it).
4. **Open the same folder in Antigravity** (File → Open Folder). Android Studio = build/run/emulator UI; Antigravity = the agent that writes code. Keep both open; the agent runs Gradle from the terminal.
5. **Copy these files into the project root** (same folder as `settings.gradle.kts`):

```
CodeMate/
├── AGENTS.md                         ← auto-read by Antigravity (keep < 12,000 chars)
├── 01-MASTER-PROMPT.md               ← you paste this as the first message
├── Automated-Verification-Loops.md
├── LOOP-Instructions.md
├── Recommended-Workflow.md           (this file)
├── skills.md
├── scripts/                          ← agent creates verify.sh / verify.ps1 in Phase 0
├── docs/                             ← agent creates PROJECT_STATE.md, DECISIONS.md, ...
└── .agents/                          ← optional: workflows/skills split out (see §5)
```
6. `git init` (or let Phase 0 do it). Add a `.gitignore` (Phase 0 creates a proper one).

> `AGENTS.md` is the cross-tool rules file Antigravity reads from the project root. If you need Antigravity-only overrides, a `GEMINI.md` in the root takes precedence over it.
> Workspace folder naming differs between Antigravity versions (`.agent/` vs `.agents/`). Use whichever your installed version creates; if unsure, ask the agent to check its own docs.

## 2. Antigravity settings I recommend
- **Mode:** *Planning* for each phase start (gives you an Implementation Plan artifact to approve); *Fast* for tiny tweaks/typos.
- **Model:** use the strongest model available for planning/architecture (Phases 0, 4, 5) and a faster one for repetitive UI work. If you get stuck on a hard bug, switch the model for that one conversation.
- **Terminal command policy:** "request review"/allow-list style — allow `gradlew`, `adb`, `git status/diff/add/commit`; deny `rm -rf`, `git push --force`. (Setting names vary by version.)
- **Artifact review policy:** ask for review on **plans**; let it proceed on walkthroughs/screenshots.
- **One conversation per phase.** Long chats lose context. State lives in `docs/PROJECT_STATE.md`, not in chat history.
- Keep Android Studio's emulator running so `adb devices` shows a device before `full` verification.

## 3. The per-phase ritual (repeat 12 times)

| Step | Who | What |
|---|---|---|
| 1 | You | New Antigravity conversation. Type: `Start Phase N. Follow LOOP-Instructions.md.` (Phase 0 only: paste the whole master prompt first.) |
| 2 | Agent | Reads state files, posts **Implementation Plan** artifact |
| 3 | You | Skim plan (2 min). Approve, or reply with changes. (Required for Phases 0, 4, 5.) |
| 4 | Agent | Implements in slices → runs verify → fixes → commits |
| 5 | You | Run the app yourself on the emulator/phone for 2–3 minutes. Test the happy path **and** break it once. |
| 6 | Agent | Posts phase report + screenshots + exit checklist |
| 7 | You | Reply `Phase N accepted` (agent tags `phase-NN-done`) or list issues → agent runs a **fix loop** |
| 8 | You | Close the conversation. Next phase = new conversation. |

**Continuous mode (optional, when you trust the loop):** tell the agent `Continuous mode ON for phases N–M. Stop on any red gate, any NOT VERIFIED item that needs me, or any item in AGENTS.md §11.`
Use it for Phases 1–3 and 6–9, not for 4–5.

## 4. Time plan (example for a 48-hour hackathon; compress if shorter)

| Block | Phases | Hours |
|---|---|---|
| Hour 0–3 | 0, 1 | 3 |
| Hour 3–8 | 2, 3 | 5 |
| Hour 8–14 | 4 (cloud chat) | 6 |
| Hour 14–22 | 5 (offline engine) — highest risk, start early | 8 |
| Hour 22–30 | 6, 7 | 8 |
| Hour 30–36 | 8, 9 | 6 |
| Hour 36–42 | 10 polish | 6 |
| Hour 42–48 | 11 hardening + demo rehearsal | 6 |

**Risk rule:** if Phase 5 (offline LLM) isn't working by the end of its time box, ship the Lite/Standard model via **side-loading** and move on — do not let it eat the schedule.
**Cut-line:** MVP = Phases 0–7 + 9. Drop Phase 10 first, then simplify Phase 8.

## 5. Optional: Antigravity workflows (slash commands)
Create these small files (each well under the 12,000-char limit) so you can type `/phase`, `/verify`, `/fix`, `/handoff`.

`.agents/workflows/phase.md`
```
---
description: Start or continue a CodeMate phase
---
1. Read docs/PROJECT_STATE.md, docs/DECISIONS.md, docs/BLOCKERS.md, AGENTS.md, LOOP-Instructions.md.
2. Identify the current phase (or the phase number I gave) from 01-MASTER-PROMPT.md §10.
3. Produce an Implementation Plan artifact and wait for my approval if the phase requires it.
4. Run the PHASE LOOP from LOOP-Instructions.md until the exit gate is green or the fix budget is spent.
5. Post the phase report in the format of AGENTS.md §10.
```
`.agents/workflows/verify.md`
```
---
description: Run the verification gates and report
---
Run scripts/verify (fast, or full if I said "full"). Report each gate with the command and final result line. Do not fix anything unless I say /fix.
```
`.agents/workflows/fix.md`
```
---
description: Fix loop for failing gates or reported bugs
---
Run the FIX LOOP from LOOP-Instructions.md: reproduce, diagnose root cause, minimal fix + regression test, re-verify. Max 5 cycles, then write docs/BLOCKERS.md and stop.
```
`.agents/workflows/handoff.md`
```
---
description: Save state before ending the conversation
---
Update docs/PROJECT_STATE.md, docs/DECISIONS.md, docs/LOOP_LOG.md, commit, and print the one-line command I should type to start the next conversation.
```
Skills: either keep the single `skills.md` (agent reads it when told by `AGENTS.md`), or split each `## SKILL:` section into `.agents/skills/<name>/SKILL.md`.

## 6. Git strategy
- `main` always builds. Branch `phase/NN-name` per phase; merge (fast-forward or squash) after you accept; tag `phase-NN-done`.
- If a phase goes wrong: `git switch main` and restart the phase on a fresh branch — cheaper than untangling.
- Push to GitHub yourself at the end of each phase (the agent is told not to push).

## 7. Debugging & escalation ladder
1. Agent fix loop (≤ 5 cycles) → 2. `docs/BLOCKERS.md` written → 3. You paste the first Gradle/logcat error + BLOCKERS summary into a **new** conversation (maybe a different model) → 4. Check official docs yourself for the library in question → 5. Simplify the feature (cut-line).

## 8. Offline-model demo checklist (rehearse twice)
- [ ] Model file already on the demo phone (side-loaded or downloaded on Wi-Fi) and **Test model** passes.
- [ ] Phone charged, battery saver OFF, background apps closed, device not hot.
- [ ] Gemini key present for the cloud part of the demo; mobile data/Wi-Fi available.
- [ ] Demo order: Login → cloud chat (fast, polished) → Explain → Debug → Learning Mode → Quiz → Progress → **airplane mode ON → offline answer** → History/Search.
- [ ] Backup: screen-recorded video of the full flow, and a pre-built APK on a second phone.

## 9. Common mistakes to avoid
- Pasting the master prompt in every phase (use state files instead).
- Approving plans without reading the "files to be changed" list.
- Letting the agent chase flaky emulator problems — restart the AVD, cold boot, move on.
- Testing offline mode only on the emulator — verify on a real phone.
- Committing `local.properties` or the model file.
