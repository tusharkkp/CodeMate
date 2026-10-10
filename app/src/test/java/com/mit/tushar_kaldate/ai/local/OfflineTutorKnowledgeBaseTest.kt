package com.mit.tushar_kaldate.ai.local

import com.mit.tushar_kaldate.ai.engine.LlmRequest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OfflineTutorKnowledgeBaseTest {

    private val dummyModelFile = File("qwen2.5-0.5b-instruct.litertlm")

    @Test
    fun `generateResponse for BFS returns Breadth-First Search explanation and code`() {
        val request = LlmRequest(prompt = "what is bfs")
        val response = OfflineTutorKnowledgeBase.generateResponse(request, dummyModelFile)

        assertTrue(response.contains("Breadth-First Search (BFS)"))
        assertTrue(response.contains("FIFO Queue"))
        assertTrue(response.contains("O(V + E)"))
        assertFalse(response.contains("Processing prompt: what is bfs"))
    }

    @Test
    fun `generateResponse for DFS returns Depth-First Search explanation`() {
        val request = LlmRequest(prompt = "explain dfs algorithm")
        val response = OfflineTutorKnowledgeBase.generateResponse(request, dummyModelFile)

        assertTrue(response.contains("Depth-First Search (DFS)"))
        assertTrue(response.contains("backtracking"))
        assertTrue(response.contains("O(V + E)"))
    }

    @Test
    fun `generateResponse respects requested programming language`() {
        val pythonRequest = LlmRequest(prompt = "how does bfs work in python")
        val pythonResponse = OfflineTutorKnowledgeBase.generateResponse(pythonRequest, dummyModelFile)
        assertTrue(pythonResponse.contains("```python"))
        assertTrue(pythonResponse.contains("from collections import deque"))

        val cppRequest = LlmRequest(prompt = "explain bfs in cpp")
        val cppResponse = OfflineTutorKnowledgeBase.generateResponse(cppRequest, dummyModelFile)
        assertTrue(cppResponse.contains("```cpp"))
        assertTrue(cppResponse.contains("std::queue"))
    }

    @Test
    fun `generateResponse for greetings returns friendly tutor welcome`() {
        val request = LlmRequest(prompt = "hello")
        val response = OfflineTutorKnowledgeBase.generateResponse(request, dummyModelFile)

        assertTrue(response.contains("CodeMate"))
        assertTrue(response.contains("100% offline"))
    }

    @Test
    fun `generateResponse for arbitrary topic returns structured guide not generic stub`() {
        val request = LlmRequest(prompt = "explain garbage collection")
        val response = OfflineTutorKnowledgeBase.generateResponse(request, dummyModelFile)

        assertTrue(response.contains("Garbage collection"))
        assertTrue(response.contains("Concept Overview"))
        assertFalse(response.contains("Processing prompt:"))
    }

    @Test
    fun `generateResponse for datatypes in cpp returns comprehensive C++ types guide`() {
        val request = LlmRequest(prompt = "datatypes in cpp")
        val response = OfflineTutorKnowledgeBase.generateResponse(request, dummyModelFile)

        assertTrue(response.contains("Data Types in C++"))
        assertTrue(response.contains("```cpp"))
        assertTrue(response.contains("#include <iostream>"))
        assertTrue(response.contains("std::cout"))
        assertFalse(response.contains("public class Example"))
    }

    @Test
    fun `generateResponse for sum 2 numbers returns complete runnable program`() {
        val request = LlmRequest(prompt = "write a program to sum 2 numbers")
        val response = OfflineTutorKnowledgeBase.generateResponse(request, dummyModelFile)

        assertTrue(response.contains("Sum Two Numbers"))
        assertTrue(response.contains("num1 + num2"))
        assertFalse(response.contains("Executing write a program to sum 2 numbers solution"))
    }
}
