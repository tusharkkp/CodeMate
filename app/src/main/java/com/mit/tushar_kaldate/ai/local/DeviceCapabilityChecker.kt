package com.mit.tushar_kaldate.ai.local

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
import java.io.File

data class DeviceCapability(
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val freeStorageBytes: Long,
    val recommendedTier: ModelTier,
    val canRunOfflineLlm: Boolean,
    val warningMessage: String? = null
) {
    val totalRamGb: Double get() = totalRamBytes.toDouble() / (1024 * 1024 * 1024)
    val freeStorageGb: Double get() = freeStorageBytes.toDouble() / (1024 * 1024 * 1024)
}

/**
 * Inspects device physical RAM and free filesystem storage to recommend
 * the most suitable on-device model tier and prevent out-of-memory crashes.
 */
class DeviceCapabilityChecker(private val context: Context) {

    fun checkCapability(): DeviceCapability {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        activityManager?.getMemoryInfo(memoryInfo)

        val rawTotalRam = memoryInfo.totalMem
        val rawAvailRam = memoryInfo.availMem

        val totalRam = if (rawTotalRam > 0L) {
            rawTotalRam
        } else {
            val jvmMem = Runtime.getRuntime().maxMemory()
            if (jvmMem > 0L) jvmMem.coerceAtLeast(4L * GB) else 4L * GB
        }

        val availRam = if (rawAvailRam > 0L) {
            rawAvailRam
        } else {
            totalRam / 2
        }

        val storageDir: File = context.getExternalFilesDir("models") ?: context.filesDir
        val freeStorage = when {
            storageDir.usableSpace > 0L -> storageDir.usableSpace
            else -> try {
                val stat = StatFs(storageDir.path)
                val bytes = stat.availableBytes
                if (bytes > 0L) bytes else 10L * 1024 * 1024 * 1024
            } catch (e: Exception) {
                10L * 1024 * 1024 * 1024
            }
        }

        return evaluate(totalRam, availRam, freeStorage)
    }

    companion object {
        const val GB = 1024L * 1024 * 1024

        fun evaluate(totalRam: Long, availRam: Long, freeStorage: Long): DeviceCapability {
            val recommendedTier = when {
                totalRam >= 5.5 * GB && freeStorage >= 3.5 * GB -> ModelTier.QUALITY
                totalRam >= 3.5 * GB && freeStorage >= 2.0 * GB -> ModelTier.STANDARD
                else -> ModelTier.LITE
            }

            val (canRun, warning) = when {
                totalRam < 2.5 * GB -> Pair(
                    false,
                    "Device RAM is under 3 GB. Running on-device LLMs may cause crashes."
                )
                freeStorage < 0.8 * GB -> Pair(
                    false,
                    "Less than 800 MB storage available. Free up space to install models."
                )
                else -> Pair(true, null)
            }

            return DeviceCapability(
                totalRamBytes = totalRam,
                availableRamBytes = availRam,
                freeStorageBytes = freeStorage,
                recommendedTier = recommendedTier,
                canRunOfflineLlm = canRun,
                warningMessage = warning
            )
        }
        fun evaluate(context: Context): DeviceCapability = DeviceCapabilityChecker(context).checkCapability()
    }
}
