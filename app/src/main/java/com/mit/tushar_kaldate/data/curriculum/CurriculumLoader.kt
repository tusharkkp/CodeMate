package com.mit.tushar_kaldate.data.curriculum

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

class CurriculumLoader(
    private val context: Context,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
) {

    private var cachedData: CurriculumData? = null

    fun loadCurriculum(): CurriculumData {
        cachedData?.let { return it }

        return try {
            context.assets.open("curriculum.json").use { stream ->
                val content = InputStreamReader(stream).readText()
                val parsed = json.decodeFromString<CurriculumData>(content)
                cachedData = parsed
                parsed
            }
        } catch (e: Exception) {
            CurriculumData()
        }
    }

    fun getLanguages(): List<CurriculumLanguage> {
        return loadCurriculum().languages
    }

    fun getTopicsForLanguage(languageId: String): List<CurriculumTopic> {
        val normalized = languageId.trim().lowercase()
        return getLanguages().firstOrNull {
            it.id.lowercase() == normalized || it.name.lowercase() == normalized
        }?.topics ?: emptyList()
    }

    fun getTopic(languageId: String, topicId: String): CurriculumTopic? {
        return getTopicsForLanguage(languageId).firstOrNull { it.id == topicId }
    }

    fun searchTopics(query: String): List<CurriculumTopic> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        val results = mutableListOf<CurriculumTopic>()
        for (lang in getLanguages()) {
            for (topic in lang.topics) {
                if (topic.title.lowercase().contains(q) ||
                    topic.description.lowercase().contains(q) ||
                    topic.concepts.any { it.lowercase().contains(q) }
                ) {
                    results.add(topic)
                }
            }
        }
        return results
    }
}
