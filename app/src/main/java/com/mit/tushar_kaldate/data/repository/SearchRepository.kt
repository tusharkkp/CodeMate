package com.mit.tushar_kaldate.data.repository

import com.mit.tushar_kaldate.data.curriculum.CurriculumLoader
import com.mit.tushar_kaldate.data.curriculum.CurriculumTopic
import com.mit.tushar_kaldate.data.local.dao.ConversationDao
import com.mit.tushar_kaldate.data.local.dao.MessageDao
import com.mit.tushar_kaldate.data.local.entity.ConversationEntity
import com.mit.tushar_kaldate.data.local.entity.MessageEntity

enum class SearchResultType {
    MESSAGE,
    CONVERSATION,
    TOPIC
}

data class SearchResultItem(
    val type: SearchResultType,
    val id: String,
    val title: String,
    val snippet: String,
    val language: String? = null,
    val targetId: Long? = null
)

class SearchRepository(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val curriculumLoader: CurriculumLoader
) {

    suspend fun searchAll(userId: Long, query: String): List<SearchResultItem> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        val results = mutableListOf<SearchResultItem>()

        // 1. Conversation titles
        val convs = conversationDao.searchTitles(userId, q)
        for (conv in convs) {
            results.add(
                SearchResultItem(
                    type = SearchResultType.CONVERSATION,
                    id = "conv_${conv.id}",
                    title = conv.title,
                    snippet = "Mode: ${conv.mode.name} • ${conv.language}",
                    language = conv.language,
                    targetId = conv.id
                )
            )
        }

        // 2. FTS messages search
        val sanitizedFtsQuery = sanitizeFtsQuery(q)
        if (sanitizedFtsQuery.isNotBlank()) {
            val messages = messageDao.searchFts(userId, sanitizedFtsQuery)
            for (msg in messages) {
                results.add(
                    SearchResultItem(
                        type = SearchResultType.MESSAGE,
                        id = "msg_${msg.id}",
                        title = "${msg.role.name} Message",
                        snippet = msg.content.take(120),
                        targetId = msg.conversationId
                    )
                )
            }
        }

        // 3. Curriculum topics
        val topics = curriculumLoader.searchTopics(q)
        for (topic in topics) {
            results.add(
                SearchResultItem(
                    type = SearchResultType.TOPIC,
                    id = "topic_${topic.id}",
                    title = topic.title,
                    snippet = topic.description,
                    language = null
                )
            )
        }

        return results
    }

    suspend fun searchMessages(userId: Long, query: String): List<MessageEntity> {
        val sanitized = sanitizeFtsQuery(query)
        if (sanitized.isBlank()) return emptyList()
        return messageDao.searchFts(userId, sanitized)
    }

    suspend fun searchConversations(userId: Long, query: String): List<ConversationEntity> {
        return conversationDao.searchTitles(userId, query.trim())
    }

    fun searchCurriculum(query: String): List<CurriculumTopic> {
        return curriculumLoader.searchTopics(query)
    }

    private fun sanitizeFtsQuery(query: String): String {
        val cleaned = query.replace("\"", "").replace("'", "").trim()
        val terms = cleaned.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (terms.isEmpty()) return ""
        // Append wildcard for prefix match
        return terms.joinToString(" ") { "$it*" }
    }
}
