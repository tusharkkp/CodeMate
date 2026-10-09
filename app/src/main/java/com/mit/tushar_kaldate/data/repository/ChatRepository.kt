package com.mit.tushar_kaldate.data.repository

import com.mit.tushar_kaldate.data.local.dao.ConversationDao
import com.mit.tushar_kaldate.data.local.dao.MessageDao
import com.mit.tushar_kaldate.data.local.entity.ConversationEntity
import com.mit.tushar_kaldate.data.local.entity.MessageEntity
import com.mit.tushar_kaldate.data.model.EngineType
import com.mit.tushar_kaldate.data.model.LearnerLevel
import com.mit.tushar_kaldate.data.model.MessageRole
import com.mit.tushar_kaldate.data.model.TutorMode
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {

    fun observeConversations(userId: Long): Flow<List<ConversationEntity>> {
        return conversationDao.observeConversationsByUser(userId)
    }

    fun observePinnedConversations(userId: Long): Flow<List<ConversationEntity>> {
        return conversationDao.observePinnedConversations(userId)
    }

    fun observeConversation(conversationId: Long): Flow<ConversationEntity?> {
        return conversationDao.observeConversationById(conversationId)
    }

    suspend fun getConversation(conversationId: Long): ConversationEntity? {
        return conversationDao.getConversationById(conversationId)
    }

    suspend fun createConversation(
        userId: Long,
        title: String,
        mode: TutorMode = TutorMode.CHAT,
        language: String = "Python",
        level: LearnerLevel = LearnerLevel.BEGINNER,
        engine: EngineType = EngineType.CLOUD
    ): Long {
        val entity = ConversationEntity(
            userId = userId,
            title = title,
            mode = mode,
            language = language,
            level = level,
            engine = engine,
            pinned = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return conversationDao.insertConversation(entity)
    }

    suspend fun updateConversationTitle(id: Long, title: String) {
        conversationDao.updateTitle(id, title)
    }

    suspend fun setPinned(id: Long, pinned: Boolean) {
        conversationDao.updatePinned(id, pinned)
    }

    suspend fun deleteConversation(id: Long) {
        conversationDao.deleteConversationById(id)
    }

    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> {
        return messageDao.observeMessagesForConversation(conversationId)
    }

    suspend fun getMessages(conversationId: Long): List<MessageEntity> {
        return messageDao.getMessagesForConversation(conversationId)
    }

    suspend fun getRecentMessages(conversationId: Long, limit: Int): List<MessageEntity> {
        return messageDao.getRecentMessages(conversationId, limit)
    }

    suspend fun addMessage(
        conversationId: Long,
        role: MessageRole,
        content: String,
        engineUsed: EngineType? = null,
        tokensEstimate: Int = 0
    ): Long {
        val message = MessageEntity(
            conversationId = conversationId,
            role = role,
            content = content,
            engineUsed = engineUsed,
            tokensEstimate = tokensEstimate,
            createdAt = System.currentTimeMillis()
        )
        val messageId = messageDao.insertMessage(message)
        // Bump conversation updatedAt
        conversationDao.getConversationById(conversationId)?.let { conv ->
            conversationDao.updateConversation(conv.copy(updatedAt = System.currentTimeMillis()))
        }
        return messageId
    }
}
