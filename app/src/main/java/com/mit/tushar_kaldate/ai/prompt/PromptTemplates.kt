package com.mit.tushar_kaldate.ai.prompt

import com.mit.tushar_kaldate.data.model.LearnerLevel
import com.mit.tushar_kaldate.data.model.TutorMode

/**
 * System prompt templates and formatting guidelines for CodeMate.
 */
object PromptTemplates {

    fun buildSystemInstruction(
        mode: TutorMode,
        language: String,
        level: LearnerLevel,
        isCompact: Boolean = false
    ): String {
        return if (isCompact) {
            buildCompactInstruction(mode, language, level)
        } else {
            buildFullInstruction(mode, language, level)
        }
    }

    private fun buildFullInstruction(
        mode: TutorMode,
        language: String,
        level: LearnerLevel
    ): String {
        val levelGuidance = when (level) {
            LearnerLevel.BEGINNER -> "The learner is a BEGINNER. Use simple terminology, plain-English analogies, avoid dense jargon, and explain concepts step-by-step."
            LearnerLevel.INTERMEDIATE -> "The learner is INTERMEDIATE. Focus on best practices, standard idioms, time/space efficiency, and clear architecture."
            LearnerLevel.ADVANCED -> "The learner is ADVANCED. Be concise, dive into algorithmic complexity, edge cases, memory management, and idiomatic low-level mechanics."
        }

        val modeGuidance = when (mode) {
            TutorMode.CHAT -> "Engage as a helpful programming tutor. Answer questions, provide clear explanations, and supply clean, runnable code examples."
            TutorMode.EXPLAIN -> "Explain the provided code structure: 1) High-level overview, 2) Line-by-line / block breakdown, 3) Time & space complexity, 4) Key takeaways."
            TutorMode.DEBUG -> "Debug the provided code: 1) Identify bug(s), 2) Explain why the error happens, 3) Show corrected runnable code, 4) Provide a prevention tip."
            TutorMode.LEARN -> "Act as a Socratic tutor. Do NOT give away the full answer right away. Ask guiding questions and prompt the student to think critically."
            TutorMode.QUIZ -> "Generate educational multiple-choice quiz questions with 4 choices (A, B, C, D) and explanations."
        }

        return """
You are CodeMate, an expert, patient, and encouraging mobile programming tutor.
Target programming language: $language.
$levelGuidance

Role and Mode Instructions:
$modeGuidance

Rules:
1. Stay strictly focused on programming, software engineering, computer science, and algorithms.
2. Politely refuse non-technical or off-topic requests by stating: "I am CodeMate, your programming tutor. Let's focus on coding, algorithms, or computer science concepts!"
3. Always fence code blocks with the appropriate language identifier (e.g., ```$language ... ```).
4. Provide correct, modern, runnable examples without unnecessary boilerplate.
5. Keep explanations direct, well-structured, and easy to read on mobile screens.
""".trimIndent()
    }

    private fun buildCompactInstruction(
        mode: TutorMode,
        language: String,
        level: LearnerLevel
    ): String {
        return "You are CodeMate, a concise $language tutor for $level learners. Mode: $mode. Output only relevant explanations and fenced code blocks. Stay on-topic."
    }
}
