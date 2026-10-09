package com.mit.tushar_kaldate.ui.models

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.mit.tushar_kaldate.ai.local.DeviceCapability
import com.mit.tushar_kaldate.ai.local.DeviceCapabilityChecker
import com.mit.tushar_kaldate.ai.local.LiteRtLocalEngine
import com.mit.tushar_kaldate.ai.local.ModelCatalog
import com.mit.tushar_kaldate.ai.local.ModelManager
import com.mit.tushar_kaldate.ai.local.ModelTier
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModelManagerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var modelManager: ModelManager
    private lateinit var capabilityChecker: DeviceCapabilityChecker
    private lateinit var localEngine: LiteRtLocalEngine
    private lateinit var workManager: WorkManager
    private lateinit var viewModel: ModelManagerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        modelManager = mockk(relaxed = true)
        capabilityChecker = mockk(relaxed = true)
        localEngine = mockk(relaxed = true)
        workManager = mockk(relaxed = true)

        every { capabilityChecker.checkCapability() } returns DeviceCapability(
            totalRamBytes = 8L * 1024 * 1024 * 1024,
            availableRamBytes = 4L * 1024 * 1024 * 1024,
            freeStorageBytes = 20L * 1024 * 1024 * 1024,
            recommendedTier = ModelTier.STANDARD,
            canRunOfflineLlm = true,
            warningMessage = null
        )

        every { modelManager.getActiveModelFile() } returns null
        every { modelManager.isModelInstalled(any<String>()) } returns false
        every { modelManager.isModelInstalled(any<com.mit.tushar_kaldate.ai.local.ModelTierInfo>()) } returns false

        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns flowOf(emptyList())

        viewModel = ModelManagerViewModel(
            modelManager = modelManager,
            capabilityChecker = capabilityChecker,
            localEngine = localEngine,
            workManager = workManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads catalog and marks recommended tier`() {
        val state = viewModel.uiState.value
        assertNotNull(state.capability)
        assertEquals(ModelTier.STANDARD, state.capability?.recommendedTier)
        assertEquals(3, state.models.size)

        val standardItem = state.models.first { it.tierInfo.tier == ModelTier.STANDARD }
        assertTrue(standardItem.isRecommended)

        val liteItem = state.models.first { it.tierInfo.tier == ModelTier.LITE }
        assertFalse(liteItem.isRecommended)
    }

    @Test
    fun `setWifiOnly updates wifiOnly flag`() {
        assertTrue(viewModel.uiState.value.wifiOnly)
        viewModel.setWifiOnly(false)
        assertFalse(viewModel.uiState.value.wifiOnly)
    }

    @Test
    fun `startDownload enqueues WorkManager work`() {
        val model = ModelCatalog.STANDARD_MODEL
        viewModel.startDownload(model)

        verify {
            workManager.enqueueUniqueWork(
                "model_download_${model.id}",
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>()
            )
        }

        val downloadingItem = viewModel.uiState.value.models.first { it.tierInfo.id == model.id }
        assertTrue(downloadingItem.isDownloading)
    }

    @Test
    fun `cancelDownload cancels unique work`() {
        val model = ModelCatalog.STANDARD_MODEL
        viewModel.cancelDownload(model)

        verify {
            workManager.cancelUniqueWork("model_download_${model.id}")
        }
    }

    @Test
    fun `deleteModel unloads engine and refreshes catalog`() {
        val model = ModelCatalog.STANDARD_MODEL
        every { modelManager.deleteModel(model) } returns true

        viewModel.deleteModel(model)

        verify { modelManager.deleteModel(model) }
        verify { localEngine.unloadModel() }
    }
}
