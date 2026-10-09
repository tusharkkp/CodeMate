package com.mit.tushar_kaldate.ai.engine

import com.mit.tushar_kaldate.data.model.EngineType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class EngineRouterTest {

    private lateinit var cloudEngine: LlmEngine
    private lateinit var localEngine: LlmEngine
    private lateinit var engineRouter: EngineRouter

    @Before
    fun setUp() {
        cloudEngine = mockk(relaxed = true)
        localEngine = mockk(relaxed = true)
        engineRouter = EngineRouter(cloudEngine, localEngine)
    }

    @Test
    fun `resolveEngine with CLOUD preference always returns cloudEngine`() = runBlocking {
        val resolved = engineRouter.resolveEngine(EngineType.CLOUD)
        assertEquals(cloudEngine, resolved)
    }

    @Test
    fun `resolveEngine with LOCAL preference returns localEngine`() = runBlocking {
        val resolved = engineRouter.resolveEngine(EngineType.LOCAL)
        assertEquals(localEngine, resolved)
    }

    @Test
    fun `resolveEngine with LOCAL preference falls back to cloudEngine when local is null`() = runBlocking {
        val routerNoLocal = EngineRouter(cloudEngine, localEngine = null)
        val resolved = routerNoLocal.resolveEngine(EngineType.LOCAL)
        assertEquals(cloudEngine, resolved)
    }

    @Test
    fun `resolveEngine with AUTO returns cloudEngine when cloud is Ready`() = runBlocking {
        coEvery { cloudEngine.isAvailable() } returns EngineStatus.Ready
        coEvery { localEngine.isAvailable() } returns EngineStatus.Ready

        val resolved = engineRouter.resolveEngine(EngineType.AUTO)
        assertEquals(cloudEngine, resolved)
    }

    @Test
    fun `resolveEngine with AUTO falls back to localEngine when cloud is Unavailable but local is Ready`() = runBlocking {
        coEvery { cloudEngine.isAvailable() } returns EngineStatus.Unavailable("No network")
        coEvery { localEngine.isAvailable() } returns EngineStatus.Ready

        val resolved = engineRouter.resolveEngine(EngineType.AUTO)
        assertEquals(localEngine, resolved)
    }

    @Test
    fun `resolveEngine with AUTO falls back to cloudEngine when neither is Ready`() = runBlocking {
        coEvery { cloudEngine.isAvailable() } returns EngineStatus.Unavailable("No network")
        coEvery { localEngine.isAvailable() } returns EngineStatus.NotConfigured("No model installed")

        val resolved = engineRouter.resolveEngine(EngineType.AUTO)
        assertEquals(cloudEngine, resolved)
    }
}
