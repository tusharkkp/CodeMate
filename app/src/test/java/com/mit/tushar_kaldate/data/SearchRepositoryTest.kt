package com.mit.tushar_kaldate.data

import com.mit.tushar_kaldate.data.curriculum.CurriculumLoader
import com.mit.tushar_kaldate.data.curriculum.CurriculumTopic
import com.mit.tushar_kaldate.data.local.dao.ConversationDao
import com.mit.tushar_kaldate.data.local.dao.MessageDao
import com.mit.tushar_kaldate.data.local.entity.ConversationEntity
import com.mit.tushar_kaldate.data.local.entity.MessageEntity
import com.mit.tushar_kaldate.data.model.MessageRole
import com.mit.tushar_kaldate.data.model.TutorMode
import com.mit.tushar_kaldate.data.repository.SearchResultType
import com.mit.tushar_kaldate.data.repository.SearchRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRepositoryTest {

    private val messageDao: MessageDao = mockk(relaxed = true)
    private val conversationDao: ConversationDao = mockk(relaxed = true)
    private val curriculumLoader: CurriculumLoader = mockk(relaxed = true)
    private val repository = SearchRepository(messageDao, conversationDao, curriculumLoader)

    @Test
    fun searchAll_returnsEmptyOnBlankQuery() = runTest {
        val results = repository.searchAll(1L, "   ")
        assertTrue(results.isEmpty())
    }

    @Test
    fun searchAll_aggregatesConversationsMessagesAndCurriculum() = runTest {
        val conv = ConversationEntity(
            id = 5L,
            userId = 1L,
            title = "Python Recursion Discussion",
            mode = TutorMode.CHAT
        )
        val msg = MessageEntity(
            id = 12L,
            conversationId = 5L,
            role = MessageRole.ASSISTANT,
            content = "Recursion base case is essential for termination."
        )
        val topic = CurriculumTopic(
            id = "recursion",
            title = "Recursion in Python",
            description = "Functions that call themselves"
        )

        coEvery { conversationDao.searchTitles(1L, "recursion") } returns listOf(conv)
        coEvery { messageDao.searchFts(1L, "recursion*") } returns listOf(msg)
        every { curriculumLoader.searchTopics("recursion") } returns listOf(topic)

        val results = repository.searchAll(1L, "recursion")

        assertEquals(3, results.size)
        assertTrue(results.any { it.type == SearchResultType.CONVERSATION && it.targetId == 5L })
        assertTrue(results.any { it.type == SearchResultType.MESSAGE && it.targetId == 5L })
        assertTrue(results.any { it.type == SearchResultType.TOPIC && it.id == "topic_recursion" })
    }
}
