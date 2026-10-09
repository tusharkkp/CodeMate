package com.mit.tushar_kaldate.data.repository

import com.mit.tushar_kaldate.data.local.dao.DailyActivityDao
import com.mit.tushar_kaldate.data.local.dao.TopicProgressDao
import com.mit.tushar_kaldate.data.local.entity.DailyActivityEntity
import com.mit.tushar_kaldate.data.local.entity.TopicProgressEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import kotlin.math.roundToInt

class ProgressRepository(
    private val topicProgressDao: TopicProgressDao,
    private val dailyActivityDao: DailyActivityDao
) {

    fun observeAllProgress(userId: Long): Flow<List<TopicProgressEntity>> {
        return topicProgressDao.observeAllForUser(userId)
    }

    fun observeProgressByLanguage(userId: Long, language: String): Flow<List<TopicProgressEntity>> {
        return topicProgressDao.observeByLanguage(userId, language)
    }

    fun observeTopicProgress(userId: Long, language: String, topic: String): Flow<TopicProgressEntity?> {
        return topicProgressDao.observeProgress(userId, language, topic)
    }

    /**
     * Updates topic mastery using standard rule:
     * mastery = round(0.7 * old + 0.3 * score%)
     * Clamped to 0..100
     */
    suspend fun updateMastery(
        userId: Long,
        language: String,
        topic: String,
        quizScorePercent: Double
    ): Int {
        val existing = topicProgressDao.getProgress(userId, language, topic)
        val oldMastery = existing?.mastery ?: 0
        val clampedScore = quizScorePercent.coerceIn(0.0, 100.0)
        val newMastery = (0.7 * oldMastery + 0.3 * clampedScore).roundToInt().coerceIn(0, 100)

        val updated = TopicProgressEntity(
            userId = userId,
            language = language,
            topic = topic,
            mastery = newMastery,
            sessionsCompleted = (existing?.sessionsCompleted ?: 0) + 1,
            lastStudiedAt = System.currentTimeMillis()
        )
        topicProgressDao.upsertProgress(updated)
        return newMastery
    }

    suspend fun recordActivity(
        userId: Long,
        xp: Int,
        minutes: Int,
        epochDay: Long = LocalDate.now().toEpochDay()
    ) {
        val existing = dailyActivityDao.getActivity(userId, epochDay)
        val updated = if (existing != null) {
            existing.copy(
                xp = existing.xp + xp,
                minutes = existing.minutes + minutes
            )
        } else {
            DailyActivityEntity(
                userId = userId,
                dateEpochDay = epochDay,
                xp = xp,
                minutes = minutes
            )
        }
        dailyActivityDao.recordActivity(updated)
    }

    suspend fun getTotalXp(userId: Long): Int {
        return dailyActivityDao.getTotalXp(userId) ?: 0
    }

    /**
     * Computes the continuous daily study streak.
     * If user studied today, streak starts today;
     * if not studied today but studied yesterday, streak starts yesterday;
     * otherwise 0.
     */
    suspend fun calculateStreak(
        userId: Long,
        currentEpochDay: Long = LocalDate.now().toEpochDay()
    ): Int {
        val activities = dailyActivityDao.getRecentActivities(userId, limitDays = 365)
        if (activities.isEmpty()) return 0

        val daysWithActivity = activities.map { it.dateEpochDay }.toSet()

        val startDay = when {
            daysWithActivity.contains(currentEpochDay) -> currentEpochDay
            daysWithActivity.contains(currentEpochDay - 1) -> currentEpochDay - 1
            else -> return 0
        }

        var streak = 0
        var dayToCheck = startDay
        while (daysWithActivity.contains(dayToCheck)) {
            streak++
            dayToCheck--
        }

        return streak
    }
}
