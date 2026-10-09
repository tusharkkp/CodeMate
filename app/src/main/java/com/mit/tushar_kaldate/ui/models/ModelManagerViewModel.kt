package com.mit.tushar_kaldate.ui.models

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.ai.engine.LlmChunk
import com.mit.tushar_kaldate.ai.engine.LlmRequest
import com.mit.tushar_kaldate.ai.local.DeviceCapability
import com.mit.tushar_kaldate.ai.local.DeviceCapabilityChecker
import com.mit.tushar_kaldate.ai.local.LiteRtLocalEngine
import com.mit.tushar_kaldate.ai.local.ModelCatalog
import com.mit.tushar_kaldate.ai.local.ModelDownloadWorker
import com.mit.tushar_kaldate.ai.local.ModelManager
import com.mit.tushar_kaldate.ai.local.ModelTierInfo
import com.mit.tushar_kaldate.core.ui.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class CatalogItemUiState(
    val tierInfo: ModelTierInfo,
    val isInstalled: Boolean,
    val isRecommended: Boolean,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val localFile: File? = null
)

data class ModelManagerUiState(
    val isLoading: Boolean = false,
    val capability: DeviceCapability? = null,
    val wifiOnly: Boolean = true,
    val models: List<CatalogItemUiState> = emptyList(),
    val activeModel: ModelTierInfo? = null,
    val isTestingModel: Boolean = false,
    val testResponse: String? = null
)

sealed class ModelManagerEvent {
    data class ShowMessage(val text: UiText) : ModelManagerEvent()
    data class ShowError(val text: UiText) : ModelManagerEvent()
}

class ModelManagerViewModel(
    private val modelManager: ModelManager,
    private val capabilityChecker: DeviceCapabilityChecker,
    private val localEngine: LiteRtLocalEngine,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelManagerUiState())
    val uiState: StateFlow<ModelManagerUiState> = _uiState.asStateFlow()

    private val _events = Channel<ModelManagerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        refreshCatalog()
        observeWorkManagerDownloads()
    }

    fun refreshCatalog() {
        val capability = capabilityChecker.checkCapability()
        val allCatalogModels = ModelCatalog.allModels

        val activeFile = modelManager.getActiveModelFile()
        val activeTierInfo = allCatalogModels.firstOrNull { modelManager.isModelInstalled(it) }

        val items = allCatalogModels.map { tierInfo ->
            val installed = modelManager.isModelInstalled(tierInfo)
            CatalogItemUiState(
                tierInfo = tierInfo,
                isInstalled = installed,
                isRecommended = tierInfo.tier == capability.recommendedTier,
                isDownloading = false,
                downloadProgress = 0,
                localFile = if (installed) modelManager.getModelFile(tierInfo) else null
            )
        }

        _uiState.update {
            it.copy(
                capability = capability,
                models = items,
                activeModel = activeTierInfo
            )
        }
    }

    fun setWifiOnly(enabled: Boolean) {
        _uiState.update { it.copy(wifiOnly = enabled) }
    }

    fun startDownload(tierInfo: ModelTierInfo) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (_uiState.value.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val downloadWork = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    ModelDownloadWorker.KEY_MODEL_ID to tierInfo.id,
                    ModelDownloadWorker.KEY_DOWNLOAD_URL to tierInfo.downloadUrl,
                    ModelDownloadWorker.KEY_TARGET_FILENAME to tierInfo.filename,
                    ModelDownloadWorker.KEY_TOTAL_SIZE to tierInfo.sizeBytes
                )
            )
            .addTag("download_${tierInfo.id}")
            .build()

        workManager.enqueueUniqueWork(
            "model_download_${tierInfo.id}",
            ExistingWorkPolicy.REPLACE,
            downloadWork
        )

        updateModelDownloading(
            tierInfo.id,
            isDownloading = true,
            progress = 0,
            downloadedBytes = 0L,
            totalBytes = tierInfo.sizeBytes
        )
    }

    fun cancelDownload(tierInfo: ModelTierInfo) {
        workManager.cancelUniqueWork("model_download_${tierInfo.id}")
        updateModelDownloading(tierInfo.id, isDownloading = false, progress = 0)
    }

    fun deleteModel(tierInfo: ModelTierInfo) {
        val deleted = modelManager.deleteModel(tierInfo)
        if (deleted) {
            localEngine.unloadModel()
            refreshCatalog()
            sendEvent(ModelManagerEvent.ShowMessage(UiText.DynamicString("Deleted ${tierInfo.displayName}")))
        } else {
            sendEvent(ModelManagerEvent.ShowError(UiText.DynamicString("Failed to delete model file")))
        }
    }

    fun importModelFromUri(uri: Uri, targetFilename: String) {
        viewModelScope.launch {
            val result = modelManager.importModelFromUri(uri, targetFilename)
            if (result.isSuccess) {
                refreshCatalog()
                sendEvent(ModelManagerEvent.ShowMessage(UiText.StringResource(R.string.models_import_success)))
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                sendEvent(ModelManagerEvent.ShowError(UiText.StringResource(R.string.models_import_failed, error)))
            }
        }
    }

    fun runTestPrompt() {
        if (_uiState.value.isTestingModel) return
        _uiState.update { it.copy(isTestingModel = true, testResponse = null) }

        viewModelScope.launch {
            val sb = java.lang.StringBuilder()
            localEngine.generate(LlmRequest(prompt = "Explain binary search in Java briefly."))
                .collect { chunk ->
                    when (chunk) {
                        is LlmChunk.Delta -> {
                            sb.append(chunk.text)
                            _uiState.update { it.copy(testResponse = sb.toString()) }
                        }
                        is LlmChunk.Done -> {
                            _uiState.update { it.copy(isTestingModel = false) }
                        }
                        is LlmChunk.Failure -> {
                            _uiState.update {
                                it.copy(
                                    isTestingModel = false,
                                    testResponse = "Error: ${chunk.error.rawMessage}"
                                )
                            }
                        }
                    }
                }
        }
    }

    private fun observeWorkManagerDownloads() {
        ModelCatalog.allModels.forEach { model ->
            viewModelScope.launch {
                workManager.getWorkInfosForUniqueWorkFlow("model_download_${model.id}")
                    .collect { workInfos ->
                        val info = workInfos.firstOrNull() ?: return@collect
                        when (info.state) {
                            WorkInfo.State.ENQUEUED -> {
                                updateModelDownloading(
                                    model.id,
                                    isDownloading = true,
                                    progress = 0,
                                    totalBytes = model.sizeBytes
                                )
                            }
                            WorkInfo.State.RUNNING -> {
                                val progress = info.progress.getInt(ModelDownloadWorker.KEY_PROGRESS, 0)
                                val downloaded = info.progress.getLong(ModelDownloadWorker.KEY_BYTES_DOWNLOADED, 0L)
                                val total = info.progress.getLong(ModelDownloadWorker.KEY_TOTAL_BYTES, model.sizeBytes)
                                updateModelDownloading(
                                    model.id,
                                    isDownloading = true,
                                    progress = progress,
                                    downloadedBytes = downloaded,
                                    totalBytes = total
                                )
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                updateModelDownloading(model.id, isDownloading = false, progress = 100)
                                refreshCatalog()
                                sendEvent(ModelManagerEvent.ShowMessage(UiText.DynamicString("${model.displayName} ready!")))
                            }
                            WorkInfo.State.FAILED -> {
                                val error = info.outputData.getString(ModelDownloadWorker.KEY_ERROR_MESSAGE) ?: "Download failed"
                                updateModelDownloading(model.id, isDownloading = false, progress = 0)
                                sendEvent(ModelManagerEvent.ShowError(UiText.DynamicString(error)))
                            }
                            WorkInfo.State.CANCELLED -> {
                                updateModelDownloading(model.id, isDownloading = false, progress = 0)
                            }
                            else -> Unit
                        }
                    }
            }
        }
    }

    private fun updateModelDownloading(
        modelId: String,
        isDownloading: Boolean,
        progress: Int,
        downloadedBytes: Long = 0L,
        totalBytes: Long = 0L
    ) {
        _uiState.update { state ->
            val updatedList = state.models.map { item ->
                if (item.tierInfo.id == modelId) {
                    item.copy(
                        isDownloading = isDownloading,
                        downloadProgress = progress,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes
                    )
                } else {
                    item
                }
            }
            state.copy(models = updatedList)
        }
    }

    private fun sendEvent(event: ModelManagerEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }

    class Factory(
        private val context: Context,
        private val modelManager: ModelManager,
        private val capabilityChecker: DeviceCapabilityChecker,
        private val localEngine: LiteRtLocalEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val workManager = WorkManager.getInstance(context)
            return ModelManagerViewModel(
                modelManager = modelManager,
                capabilityChecker = capabilityChecker,
                localEngine = localEngine,
                workManager = workManager
            ) as T
        }
    }
}
