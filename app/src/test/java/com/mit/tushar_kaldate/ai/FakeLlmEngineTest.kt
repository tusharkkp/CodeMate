package com.mit.tushar_kaldate.ai

import app.cash.turbine.test
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.ai.engine.EngineRouter
import com.mit.tushar_kaldate.ai.engine.EngineStatus
import com.mit.tushar_kaldate.ai.engine.FakeLlmEngine
import com.mit.tushar_kaldate.ai.engine.LlmChunk
import com.mit.tushar_kaldate.ai.engine.LlmError
import com.mit.tushar_kaldate.ai.engine.LlmRequest
import com.mit.tushar_kaldate.data.model.EngineType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeLlmEngineTest {

    @Test
    fun `generate emits single delta and done in Instant mode`() = runTest {
        val fake = FakeLlmEngine(
            mode = FakeLlmEngine.Mode.Instant("Hello world!")
        )
        val request = LlmRequest(prompt = "Hi")

        fake.generate(request).test {
            val delta = awaitItem() as LlmChunk.Delta
            assertEquals("Hello world!", delta.text)

            val done = awaitItem() as LlmChunk.Done
            assertEquals(30, done.usage?.totalTokens)
            awaitComplete()
        }

        assertEquals(1, fake.recordedRequests.size)
        assertEquals("Hi", fake.recordedRequests[0].prompt)
    }

    @Test
    fun `generate emits chunks sequentially in Streaming mode`() = runTest {
        val chunks = listOf("First", " ", "Second", " ", "Done")
        val fake = FakeLlmEngine(
            mode = FakeLlmEngine.Mode.Streaming(chunks, delayMs = 0L)
        )

        fake.generate(LlmRequest(prompt = "Stream please")).test {
            assertEquals("First", (awaitItem() as LlmChunk.Delta).text)
            assertEquals(" ", (awaitItem() as LlmChunk.Delta).text)
            assertEquals("Second", (awaitItem() as LlmChunk.Delta).text)
            assertEquals(" ", (awaitItem() as LlmChunk.Delta).text)
            assertEquals("Done", (awaitItem() as LlmChunk.Delta).text)
            assertTrue(awaitItem() is LlmChunk.Done)
            awaitComplete()
        }
    }

    @Test
    fun `generate emits Failure in Error mode`() = runTest {
        val fake = FakeLlmEngine(
            mode = FakeLlmEngine.Mode.Error(LlmError.RateLimited())
        )

        fake.generate(LlmRequest(prompt = "Fail")).test {
            val failure = awaitItem() as LlmChunk.Failure
            assertTrue(failure.error is LlmError.RateLimited)
            awaitComplete()
        }
    }

    @Test
    fun `LlmError mappings return correct string resources`() {
        assertEquals(R.string.error_no_network, LlmError.NoNetwork().getStringResId())
        assertEquals(R.string.error_bad_key, LlmError.BadKey().getStringResId())
        assertEquals(R.string.error_rate_limited, LlmError.RateLimited().getStringResId())
        assertEquals(R.string.error_server, LlmError.Server(500, "Internal error").getStringResId())
        assertEquals(R.string.error_timeout, LlmError.Timeout().getStringResId())
        assertEquals(R.string.error_model_missing, LlmError.ModelMissing().getStringResId())
        assertEquals(R.string.error_safety_blocked, LlmError.SafetyBlocked("HARASSMENT").getStringResId())
        assertEquals(R.string.error_unknown, LlmError.Unknown().getStringResId())
    }

    @Test
    fun `EngineRouter resolves correctly for cloud, local, and auto`() = runTest {
        val cloudEngine = FakeLlmEngine(type = EngineType.CLOUD)
        val localEngine = FakeLlmEngine(type = EngineType.LOCAL)
        val router = EngineRouter(cloudEngine, localEngine)

        // CLOUD preference always returns cloudEngine
        assertEquals(cloudEngine, router.resolveEngine(EngineType.CLOUD))

        // LOCAL preference returns localEngine
        assertEquals(localEngine, router.resolveEngine(EngineType.LOCAL))

        // AUTO when cloud is ready returns cloud
        cloudEngine.availableStatus = EngineStatus.Ready
        assertEquals(cloudEngine, router.resolveEngine(EngineType.AUTO))

        // AUTO when cloud is unavailable falls back to local
        cloudEngine.availableStatus = EngineStatus.Unavailable("No internet")
        localEngine.availableStatus = EngineStatus.Ready
        assertEquals(localEngine, router.resolveEngine(EngineType.AUTO))
    }
}
