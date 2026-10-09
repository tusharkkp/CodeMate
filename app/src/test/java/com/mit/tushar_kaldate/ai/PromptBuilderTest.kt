package com.mit.tushar_kaldate.ai

import com.mit.tushar_kaldate.ai.config.AiConfig
import com.mit.tushar_kaldate.ai.engine.ChatTurn
import com.mit.tushar_kaldate.ai.prompt.PromptBuilder
import com.mit.tushar_kaldate.data.model.EngineType
import com.mit.tushar_kaldate.data.model.LearnerLevel
import com.mit.tushar_kaldate.data.model.MessageRole
import com.mit.tushar_kaldate.data.model.TutorMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    @Test
    fun `buildRequest trims history to cloud limit for cloud engine`() {
        val turns = (1..15).map {
            ChatTurn(
                role = if (it % 2 == 1) MessageRole.USER else MessageRole.ASSISTANT,
                content = "Message $it"
            )
        }

        val request = PromptBuilder.buildRequest(
            prompt = "What is a binary tree?",
            history = turns,
            engineType = EngineType.CLOUD
        )

        assertEquals(AiConfig.CLOUD_HISTORY_TURNS_LIMIT, request.history.size)
        assertEquals("Message 6", request.history.first().content)
        assertEquals("Message 15", request.history.last().content)
        assertEquals("What is a binary tree?", request.prompt)
    }

    @Test
    fun `buildRequest trims history to local limit and uses compact template for local engine`() {
        val turns = (1..10).map {
            ChatTurn(
                role = MessageRole.USER,
                content = "Question $it"
            )
        }

        val request = PromptBuilder.buildRequest(
            prompt = "Quick help",
            history = turns,
            engineType = EngineType.LOCAL,
            language = "Kotlin",
            level = LearnerLevel.INTERMEDIATE
        )

        assertEquals(AiConfig.LOCAL_HISTORY_TURNS_LIMIT, request.history.size)
        assertEquals("Question 7", request.history.first().content)
        assertTrue(request.systemInstruction?.contains("concise Kotlin tutor") == true)
        assertTrue(request.systemInstruction?.contains("INTERMEDIATE") == true)
    }

    @Test
    fun `buildRequest includes target language and beginner guidance in full prompt`() {
        val request = PromptBuilder.buildRequest(
            prompt = "Explain for loops",
            language = "Java",
            level = LearnerLevel.BEGINNER,
            mode = TutorMode.CHAT
        )

        val instruction = request.systemInstruction.orEmpty()
        assertTrue(instruction.contains("Target programming language: Java"))
        assertTrue(instruction.contains("BEGINNER"))
        assertTrue(instruction.contains("simple terminology"))
        assertTrue(instruction.contains("```Java"))
    }

    @Test
    fun `buildRequest includes explain mode instructions`() {
        val request = PromptBuilder.buildRequest(
            prompt = "def foo(): pass",
            mode = TutorMode.EXPLAIN,
            language = "Python"
        )

        val instruction = request.systemInstruction.orEmpty()
        assertTrue(instruction.contains("Time & space complexity"))
    }

    @Test
    fun `buildRequest includes debug mode instructions`() {
        val request = PromptBuilder.buildRequest(
            prompt = "int a = 0;",
            mode = TutorMode.DEBUG,
            language = "C++"
        )

        val instruction = request.systemInstruction.orEmpty()
        assertTrue(instruction.contains("Debug the provided code"))
        assertTrue(instruction.contains("Show corrected runnable code"))
    }
}
