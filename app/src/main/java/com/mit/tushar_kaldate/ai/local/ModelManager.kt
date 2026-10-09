package com.mit.tushar_kaldate.ai.local

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Manages filesystem operations for on-device LLM model files,
 * including installation status, side-loaded model detection,
 * file imports via SAF, and model deletion.
 */
class ModelManager(private val context: Context) {

    fun getModelsDirectory(): File {
        val dir = context.getExternalFilesDir("models") ?: File(context.filesDir, "models")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getModelFile(tierInfo: ModelTierInfo): File {
        return File(getModelsDirectory(), tierInfo.filename)
    }

    fun isModelInstalled(tierInfo: ModelTierInfo): Boolean {
        val file = getModelFile(tierInfo)
        return file.exists() && file.length() > 0
    }

    fun isModelInstalled(modelId: String): Boolean {
        val model = ModelCatalog.getModelById(modelId) ?: return false
        return isModelInstalled(model)
    }

    fun getInstalledModels(): List<File> {
        val dir = getModelsDirectory()
        return dir.listFiles { file ->
            file.isFile && (file.extension == "litertlm" || file.extension == "bin" || file.extension == "task")
        }?.toList().orEmpty()
    }

    fun getActiveModelFile(): File? {
        // Return first installed model in order of Quality -> Standard -> Lite
        return ModelCatalog.allModels
            .firstOrNull { isModelInstalled(it) }
            ?.let { getModelFile(it) }
            ?: getInstalledModels().firstOrNull()
    }

    fun deleteModel(tierInfo: ModelTierInfo): Boolean {
        val file = getModelFile(tierInfo)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    fun deleteModel(modelId: String): Boolean {
        val model = ModelCatalog.getModelById(modelId) ?: return false
        return deleteModel(model)
    }

    fun importModelFromUri(uri: Uri, targetFilename: String): Result<File> {
        return try {
            val contentResolver = context.contentResolver
            val targetFile = File(getModelsDirectory(), targetFilename)

            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return Result.failure(IllegalStateException("Unable to open input stream from URI"))

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun verifyChecksum(file: File, expectedSha256: String?): Boolean {
        if (expectedSha256.isNullOrBlank()) return true
        if (!file.exists()) return false

        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val calculated = digest.digest().joinToString("") { "%02x".format(it) }
            calculated.equals(expectedSha256, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
}
