package com.mit.tushar_kaldate.ai.local

/**
 * Hardware capability tiers for offline on-device language models.
 */
enum class ModelTier {
    QUALITY,
    STANDARD,
    LITE
}

/**
 * Metadata and distribution parameters for a cataloged on-device LLM.
 */
data class ModelTierInfo(
    val tier: ModelTier,
    val id: String,
    val displayName: String,
    val description: String,
    val filename: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val minRamBytes: Long,
    val license: String,
    val isGated: Boolean = false,
    val sha256: String? = null
)

/**
 * Catalog of recommended on-device LiteRT-LM models across 3 hardware tiers.
 */
object ModelCatalog {

    val QUALITY_MODEL = ModelTierInfo(
        tier = ModelTier.QUALITY,
        id = "gemma-2-2b-it",
        displayName = "Gemma 2 2B (Quality)",
        description = "Highest reasoning & coding capability offline. Best for devices with 6 GB+ RAM.",
        filename = "gemma-2-2b-it.litertlm",
        downloadUrl = "https://huggingface.co/litert-community/gemma-2-2b-it/resolve/main/gemma-2-2b-it.litertlm",
        sizeBytes = 3_000_000_000L, // ~3.0 GB
        minRamBytes = 6L * 1024 * 1024 * 1024, // 6 GB
        license = "Gemma Terms of Use",
        isGated = true
    )

    val STANDARD_MODEL = ModelTierInfo(
        tier = ModelTier.STANDARD,
        id = "qwen2.5-1.5b-instruct",
        displayName = "Qwen 2.5 1.5B (Standard)",
        description = "Balanced speed and accuracy. Free Apache 2.0 license, no gated download. Recommended for 4 GB+ RAM.",
        filename = "qwen2.5-1.5b-instruct.litertlm",
        downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/qwen2.5-1.5b-instruct.litertlm",
        sizeBytes = 1_600_000_000L, // ~1.6 GB
        minRamBytes = 4L * 1024 * 1024 * 1024, // 4 GB
        license = "Apache-2.0",
        isGated = false
    )

    val LITE_MODEL = ModelTierInfo(
        tier = ModelTier.LITE,
        id = "qwen2.5-0.5b-instruct",
        displayName = "Qwen 2.5 0.5B (Lite)",
        description = "Ultra-compact offline assistant for budget devices. Low memory footprint, fast generation.",
        filename = "qwen2.5-0.5b-instruct.litertlm",
        downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/qwen2.5-0.5b-instruct.litertlm",
        sizeBytes = 550_000_000L, // ~550 MB
        minRamBytes = 3L * 1024 * 1024 * 1024, // 3 GB
        license = "Apache-2.0",
        isGated = false
    )

    val allModels: List<ModelTierInfo> = listOf(QUALITY_MODEL, STANDARD_MODEL, LITE_MODEL)

    fun getModelByTier(tier: ModelTier): ModelTierInfo = when (tier) {
        ModelTier.QUALITY -> QUALITY_MODEL
        ModelTier.STANDARD -> STANDARD_MODEL
        ModelTier.LITE -> LITE_MODEL
    }

    fun getModelById(id: String): ModelTierInfo? = allModels.firstOrNull { it.id == id }

    fun getModelByFilename(filename: String): ModelTierInfo? =
        allModels.firstOrNull { it.filename.equals(filename, ignoreCase = true) }
}
