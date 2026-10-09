package com.mit.tushar_kaldate.data.repository

import com.mit.tushar_kaldate.data.local.dao.QuizDao
import com.mit.tushar_kaldate.data.local.entity.QuizAnswerEntity
import com.mit.tushar_kaldate.data.local.entity.QuizAttemptEntity
import com.mit.tushar_kaldate.data.model.Difficulty
import kotlinx.coroutines.flow.Flow

class QuizRepository(
    private val quizDao: QuizDao
) {

    fun observeAttempts(userId: Long): Flow<List<QuizAttemptEntity>> {
        return quizDao.observeAttemptsForUser(userId)
    }

    suspend fun getAttempt(attemptId: Long): QuizAttemptEntity? {
        return quizDao.getAttemptById(attemptId)
    }

    suspend fun getAnswers(attemptId: Long): List<QuizAnswerEntity> {
        return quizDao.getAnswersForAttempt(attemptId)
    }

    suspend fun recordAttempt(
        userId: Long,
        language: String,
        topic: String,
        difficulty: Difficulty,
        total: Int,
        correct: Int,
        durationMs: Long,
        answers: List<QuizAnswerEntity>
    ): Long {
        val attempt = QuizAttemptEntity(
            userId = userId,
            language = language,
            topic = topic,
            difficulty = difficulty,
            total = total,
            correct = correct,
            durationMs = durationMs,
            createdAt = System.currentTimeMillis()
        )
        val attemptId = quizDao.insertAttempt(attempt)
        val answersWithAttemptId = answers.map { it.copy(attemptId = attemptId) }
        quizDao.insertAnswers(answersWithAttemptId)
        return attemptId
    }

    suspend fun deleteAttempt(attemptId: Long) {
        quizDao.deleteAttemptById(attemptId)
    }
}
