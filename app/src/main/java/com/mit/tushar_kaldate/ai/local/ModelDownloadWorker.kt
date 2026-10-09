package com.mit.tushar_kaldate.ai.local

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mit.tushar_kaldate.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Resumable background download worker for on-device LLM model files.
 * Supports pause, resume via HTTP Range requests, and live progress notifications.
 */
class ModelDownloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_MODEL_ID = "model_id"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_TARGET_FILENAME = "target_filename"
        const val KEY_TOTAL_SIZE = "total_size"

        const val KEY_PROGRESS = "progress"
        const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_ERROR_MESSAGE = "error_message"

        private const val CHANNEL_ID = "model_download_channel"
        private const val NOTIFICATION_ID = 2001
    }

    private val modelManager = ModelManager(applicationContext)
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelId = inputData.getString(KEY_MODEL_ID).orEmpty()
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL).orEmpty()
        val filename = inputData.getString(KEY_TARGET_FILENAME).orEmpty()
        val expectedSize = inputData.getLong(KEY_TOTAL_SIZE, 0L)

        if (downloadUrl.isBlank() || filename.isBlank()) {
            return@withContext Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to "Missing download URL or filename")
            )
        }

        val targetFile = File(modelManager.getModelsDirectory(), filename)
        val partFile = File(modelManager.getModelsDirectory(), "$filename.part")

        var existingBytes = if (partFile.exists()) partFile.length() else 0L

        val requestBuilder = Request.Builder().url(downloadUrl)
        if (existingBytes > 0) {
            requestBuilder.addHeader("Range", "bytes=$existingBytes-")
        }

        createNotificationChannel()

        try {
            // Immediately report initial progress
            setProgress(
                workDataOf(
                    KEY_PROGRESS to 0,
                    KEY_BYTES_DOWNLOADED to existingBytes,
                    KEY_TOTAL_BYTES to expectedSize
                )
            )

            // Set initial foreground progress notification
            try {
                setForeground(createForegroundInfo(0, filename))
            } catch (e: Exception) {
                // Ignore foreground exception if not permitted in background state
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful && response.code != 416) {
                return@withContext Result.failure(
                    workDataOf(KEY_ERROR_MESSAGE to "HTTP ${response.code}: ${response.message}")
                )
            }

            val isAppend = response.code == 206
            if (!isAppend) {
                existingBytes = 0L
            }

            val body = response.body
            val totalBytes = if (isAppend) {
                existingBytes + (body?.contentLength() ?: (expectedSize - existingBytes))
            } else {
                body?.contentLength() ?: expectedSize
            }

            val inputStream = body?.byteStream()
                ?: return@withContext Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Empty response body"))

            FileOutputStream(partFile, isAppend).use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var currentDownloaded = existingBytes
                var lastProgress = 0
                var lastUpdateTime = System.currentTimeMillis()

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isStopped) {
                        return@withContext Result.retry()
                    }

                    outputStream.write(buffer, 0, bytesRead)
                    currentDownloaded += bytesRead

                    val progress = if (totalBytes > 0) {
                        ((currentDownloaded * 100) / totalBytes).toInt().coerceIn(0, 100)
                    } else {
                        0
                    }

                    val now = System.currentTimeMillis()
                    if (progress != lastProgress || (now - lastUpdateTime >= 300)) {
                        lastProgress = progress
                        lastUpdateTime = now
                        setProgress(
                            workDataOf(
                                KEY_PROGRESS to progress,
                                KEY_BYTES_DOWNLOADED to currentDownloaded,
                                KEY_TOTAL_BYTES to totalBytes
                            )
                        )
                        try {
                            setForeground(createForegroundInfo(progress, filename))
                        } catch (e: Exception) {
                            // Non-fatal
                        }
                    }
                }
            }

            // Successfully downloaded, rename .part to final model file
            if (partFile.renameTo(targetFile)) {
                Result.success(workDataOf("model_path" to targetFile.absolutePath))
            } else {
                Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Failed to rename completed file"))
            }
        } catch (e: IOException) {
            if (isStopped) {
                Result.retry()
            } else {
                Result.failure(workDataOf(KEY_ERROR_MESSAGE to (e.message ?: "Network error during download")))
            }
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR_MESSAGE to (e.message ?: "Download failed")))
        }
    }

    private fun createForegroundInfo(progress: Int, filename: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading AI Model")
            .setContentText("$filename ($progress%)")
            .setSmallIcon(R.drawable.ic_code)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of offline on-device model downloads"
            }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
