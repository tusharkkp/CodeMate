# LOOP Instructions for Antigravity — CodeMate

You are an autonomous but accountable engineer. These loops define HOW you work. They sit on top of `AGENTS.md` (rules) and
`Automated-Verification-Loops.md` (gates). When in doubt, be safe, small, and honest.

---

## A. PHASE LOOP (outer loop)

```
START
 1. ORIENT   Read docs/PROJECT_STATE.md → DECISIONS.md → BLOCKERS.md → master prompt §10 for this phase.
             If PROJECT_STATE.md says a different phase is current, tell the human and ask.
 2. PLAN     Create an Implementation Plan artifact:
               • goal & exit gate of this phase   • list of slices (each ≤ ~1 hour / ≤ ~300 changed lines)
               • files to create/modify           • tests to write
               • risks / things needing the human (keys, device, license)
             Wait for approval ONLY if the phase is marked [human approval]; otherwise proceed.
 3. SLICE LOOP  (repeat for each slice)  → section B
 4. PHASE GATE  Run `scripts/verify full`. If red → FIX LOOP (section C).
 5. REVIEW     Self-review the diff against the Definition of Done (AGENTS.md §12) and the phase's acceptance criteria.
               Walk the feature manually on the emulator: happy path + one failure path. Take screenshots (docs/screens/).
 6. RECORD     Update PROJECT_STATE.md, DECISIONS.md, LOOP_LOG.md. Commit. Tag `phase-NN-done` ONLY after the human says "accepted".
 7. REPORT     Post the report (AGENTS.md §10) with the exit checklist. STOP. Do not begin the next phase unless Continuous Mode is ON.
END
```

## B. SLICE LOOP (inner loop)

```
for each slice:
  1. WRITE TESTS FIRST (or alongside) for new logic — use FakeLlmEngine, in-memory Room, MockWebServer.
  2. IMPLEMENT the smallest vertical piece that makes the tests pass.
  3. VERIFY   scripts/verify fast  (G1 build, G2 lint, G3 unit).
  4. if UI changed → install on emulator, open the screen, screenshot, view the image, compare with the spec.
  5. if red → FIX LOOP (C).  if green → commit `type(scope): summary`.
  6. Append one line to docs/LOOP_LOG.md.
  7. Re-read the remaining slices; adjust the plan if you learned something (note it in DECISIONS.md).
```

## C. FIX LOOP (use for red gates, bugs, and "it crashes" reports)

```
cycle = 0
while gate is red and cycle < 5:
  cycle += 1
  1. REPRODUCE   Re-run the failing command. Capture the FIRST error + the stack frame in our package.
  2. HYPOTHESIZE One sentence: "X fails because Y". Find evidence (read the code, logcat, docs). Don't guess-patch.
  3. FIX         Minimal change at the root cause. For a bug, write the regression test first and watch it fail.
  4. RE-VERIFY   Same gate first, then the full fast set to catch regressions.
  5. LOG         `cycle k: hypothesis → change → result` in LOOP_LOG.md.
if still red after 5 cycles:
  write docs/BLOCKERS.md (symptom, exact command+error, what you tried per cycle, top 2 hypotheses, what you need from the human) and STOP.
```

## D. STUCK PROTOCOL
- **Same error signature twice** → stop patching. Re-read the relevant official docs (use the browser tool), compare with a minimal working example, and change approach.
- **Third time** → stop and ask the human. Include a minimal reproduction.
- **Gradle/AGP/Kotlin/KSP version conflicts** → read the compatibility table in official docs; change ONE version at a time; record in DECISIONS.md.
- **Native crash / OOM in offline engine** → do not catch-and-hide. Reduce model tier or context, log device RAM, report.
- **Emulator trouble** (no device, frozen) → ask the human to cold-boot the AVD; meanwhile continue with unit-testable work.
- Never "fix" by deleting tests, silencing lint, or hard-coding outputs.

## E. HONESTY PROTOCOL
Every claim in a report is tagged:
- **VERIFIED** — you ran the command/flow and saw the result (state which).
- **NOT VERIFIED** — could not run (no key / no device / no emulator); say exactly what the human must check.
- **ASSUMED** — reasoned but untested.
If a phase's exit gate has any NOT VERIFIED item, the phase is "pending human check", not "done".

## F. CONTINUOUS MODE (only when the human types it)
`Continuous mode ON for phases N–M.`
- Proceed phase to phase without waiting **only if** the full gate is green, there are no NOT VERIFIED items needing the human, and the next phase has no `[human approval]`.
- After each phase: commit, update state, post a 5-line summary, continue.
- Stop immediately on: red gate after fix budget, missing credentials, ambiguity affecting scope, any AGENTS.md §11 trigger, or 3 phases completed (post a checkpoint and wait for "continue").

## G. LONG-RUNNING TASK HYGIENE
- Keep `docs/PROJECT_STATE.md` current after every commit (it is your memory). Template:

```
# PROJECT STATE
Updated: <date time>   Branch: <branch>   Last commit: <hash msg>
Current phase: N — <name>   Status: planning | in-progress | gates-red | pending-human | done
Done: …           In progress: …           Next: …
How to run: <build/run/test commands, device used>
Known issues: …   Needs human: …
Key versions: AGP/Kotlin/Room/LiteRT-LM/Gemini model id: …
```
- Context getting long or confused? Finish the current slice, run `/handoff` (update state, commit), and tell the human to start a **new conversation**.
- Don't re-read the whole repo each time; use PROJECT_STATE.md and targeted file reads.

## H. READY-TO-PASTE COMMANDS FOR THE HUMAN

**Start a phase (new conversation):**
```
Start Phase N of CodeMate. Read AGENTS.md, docs/PROJECT_STATE.md, DECISIONS.md, BLOCKERS.md, then 01-MASTER-PROMPT.md §10 (Phase N).
Follow LOOP-Instructions.md §A. Produce the plan artifact first.
```
**Fix loop:**
```
The build/test/app is failing: <paste first error or describe bug + steps>. Run LOOP-Instructions.md §C. Add a regression test. Report with VERIFIED/NOT VERIFIED tags.
```
**Review a phase:**
```
Review Phase N against its acceptance criteria and AGENTS.md §12. List gaps, risks, and anything NOT VERIFIED. Do not change code yet.
```
**Continuous mode:**
```
Continuous mode ON for phases 6–9. Obey LOOP-Instructions.md §F.
```
**Handoff:**
```
Run handoff: update docs/PROJECT_STATE.md, DECISIONS.md, LOOP_LOG.md, commit, and give me the exact message to start the next conversation.
```
