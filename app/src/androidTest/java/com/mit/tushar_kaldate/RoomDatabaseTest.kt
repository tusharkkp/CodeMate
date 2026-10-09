package com.mit.tushar_kaldate

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mit.tushar_kaldate.data.local.CodeMateDatabase
import com.mit.tushar_kaldate.data.local.dao.ConversationDao
import com.mit.tushar_kaldate.data.local.dao.DailyActivityDao
import com.mit.tushar_kaldate.data.local.dao.MessageDao
import com.mit.tushar_kaldate.data.local.dao.QuizDao
import com.mit.tushar_kaldate.data.local.dao.TopicProgressDao
import com.mit.tushar_kaldate.data.local.dao.UserDao
import com.mit.tushar_kaldate.data.local.entity.ConversationEntity
import com.mit.tushar_kaldate.data.local.entity.DailyActivityEntity
import com.mit.tushar_kaldate.data.local.entity.MessageEntity
import com.mit.tushar_kaldate.data.local.entity.QuizAnswerEntity
import com.mit.tushar_kaldate.data.local.entity.QuizAttemptEntity
import com.mit.tushar_kaldate.data.local.entity.TopicProgressEntity
import com.mit.tushar_kaldate.data.local.entity.UserEntity
import com.mit.tushar_kaldate.data.model.Difficulty
import com.mit.tushar_kaldate.data.model.EngineType
import com.mit.tushar_kaldate.data.model.LearnerLevel
import com.mit.tushar_kaldate.data.model.MessageRole
import com.mit.tushar_kaldate.data.model.TutorMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {

    private lateinit var db: CodeMateDatabase
    private lateinit var userDao: UserDao
    private lateinit var conversationDao: ConversationDao
    private lateinit var messageDao: MessageDao
    private lateinit var quizDao: QuizDao
    private lateinit var topicProgressDao: TopicProgressDao
    private lateinit var dailyActivityDao: DailyActivityDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CodeMateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = db.userDao()
        conversationDao = db.conversationDao()
        messageDao = db.messageDao()
        quizDao = db.quizDao()
        topicProgressDao = db.topicProgressDao()
        dailyActivityDao = db.dailyActivityDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertUserAndRetrieve() = runBlocking {
        val user = UserEntity(
            name = "Test Learner",
            email = "learner@test.com",
            passwordHash = "hash123",
            salt = "salt123",
            isGuest = false
        )
        val id = userDao.insertUser(user)
        assertTrue(id > 0)

        val retrieved = userDao.getUserById(id)
        assertNotNull(retrieved)
        assertEquals("Test Learner", retrieved?.name)
        assertEquals("learner@test.com", retrieved?.email)
    }

    @Test
    fun conversationAndMessageCascadeDelete() = runBlocking {
        val userId = userDao.insertUser(
            UserEntity(name = "User", email = "u@test.com", passwordHash = "h", salt = "s")
        )

        val convId = conversationDao.insertConversation(
            ConversationEntity(
                userId = userId,
                title = "Python Session",
                mode = TutorMode.CHAT,
                language = "Python",
                level = LearnerLevel.BEGINNER,
                engine = EngineType.CLOUD
            )
        )

        messageDao.insertMessage(
            MessageEntity(
                conversationId = convId,
                role = MessageRole.USER,
                content = "How do for loops work in Python?"
            )
        )
        messageDao.insertMessage(
            MessageEntity(
                conversationId = convId,
                role = MessageRole.ASSISTANT,
                content = "For loops iterate over sequences like lists or range."
            )
        )

        val messagesBefore = messageDao.getMessagesForConversation(convId)
        assertEquals(2, messagesBefore.size)

        // Delete conversation -> should cascade delete all messages
        conversationDao.deleteConversationById(convId)

        val convAfter = conversationDao.getConversationById(convId)
        assertNull(convAfter)

        val messagesAfter = messageDao.getMessagesForConversation(convId)
        assertEquals(0, messagesAfter.size)
    }

    @Test
    fun messageFtsSearch() = runBlocking {
        val userId = userDao.insertUser(
            UserEntity(name = "User FTS", email = "fts@test.com", passwordHash = "h", salt = "s")
        )

        val convId = conversationDao.insertConversation(
            ConversationEntity(
                userId = userId,
                title = "Algorithms",
                mode = TutorMode.EXPLAIN
            )
        )

        messageDao.insertMessage(
            MessageEntity(
                conversationId = convId,
                role = MessageRole.ASSISTANT,
                content = "Quicksort has an average time complexity of O(N log N)."
            )
        )
        messageDao.insertMessage(
            MessageEntity(
                conversationId = convId,
                role = MessageRole.USER,
                content = "What about bubblesort?"
            )
        )

        val results = messageDao.searchFts(userId, "quicksort*")
        assertEquals(1, results.size)
        assertTrue(results.first().content.contains("Quicksort"))
    }

    @Test
    fun quizAttemptAndAnswers() = runBlocking {
        val userId = userDao.insertUser(
            UserEntity(name = "Quizzer", email = "quiz@test.com", passwordHash = "h", salt = "s")
        )

        val attemptId = quizDao.insertAttempt(
            QuizAttemptEntity(
                userId = userId,
                language = "Java",
                topic = "OOP",
                difficulty = Difficulty.MEDIUM,
                total = 2,
                correct = 2,
                durationMs = 5000L
            )
        )

        quizDao.insertAnswers(
            listOf(
                QuizAnswerEntity(
                    attemptId = attemptId,
                    question = "What is polymorphism?",
                    optionsJson = "[\"A\",\"B\",\"C\",\"D\"]",
                    correctIndex = 0,
                    selectedIndex = 0,
                    explanation = "Many forms",
                    concept = "Polymorphism"
                )
            )
        )

        val answers = quizDao.getAnswersForAttempt(attemptId)
        assertEquals(1, answers.size)
        assertEquals("What is polymorphism?", answers.first().question)
    }

    @Test
    fun topicProgressAndDailyActivity() = runBlocking {
        val userId = userDao.insertUser(
            UserEntity(name = "Worker", email = "work@test.com", passwordHash = "h", salt = "s")
        )

        topicProgressDao.upsertProgress(
            TopicProgressEntity(
                userId = userId,
                language = "Python",
                topic = "Loops",
                mastery = 45,
                sessionsCompleted = 1
            )
        )

        val progress = topicProgressDao.getProgress(userId, "Python", "Loops")
        assertNotNull(progress)
        assertEquals(45, progress?.mastery)

        dailyActivityDao.recordActivity(
            DailyActivityEntity(
                userId = userId,
                dateEpochDay = 19500L,
                xp = 120,
                minutes = 25
            )
        )

        val totalXp = dailyActivityDao.getTotalXp(userId)
        assertEquals(120, totalXp)
    }
}
