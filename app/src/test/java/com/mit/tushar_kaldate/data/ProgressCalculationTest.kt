package com.mit.tushar_kaldate.data

import com.mit.tushar_kaldate.data.local.dao.DailyActivityDao
import com.mit.tushar_kaldate.data.local.dao.TopicProgressDao
import com.mit.tushar_kaldate.data.local.entity.DailyActivityEntity
import com.mit.tushar_kaldate.data.local.entity.TopicProgressEntity
import com.mit.tushar_kaldate.data.repository.ProgressRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToInt

class ProgressCalculationTest {

    private val topicProgressDao: TopicProgressDao = mockk(relaxed = true)
    private val dailyActivityDao: DailyActivityDao = mockk(relaxed = true)
    private val repository = ProgressRepository(topicProgressDao, dailyActivityDao)

    @Test
    fun updateMastery_fromZeroWithPerfectScore_reachesThirty() = runTest {
        coEvery { topicProgressDao.getProgress(1L, "Python", "Loops") } returns null

        val slot = slot<TopicProgressEntity>()
        coEvery { topicProgressDao.upsertProgress(capture(slot)) } returns Unit

        val newMastery = repository.updateMastery(1L, "Python", "Loops", 100.0)

        // 0.7 * 0 + 0.3 * 100 = 30
        assertEquals(30, newMastery)
        assertEquals(30, slot.captured.mastery)
        assertEquals(1, slot.captured.sessionsCompleted)
    }

    @Test
    fun updateMastery_appliesDecayFormulaCorrectly() = runTest {
        val existing = TopicProgressEntity(
            userId = 1L,
            language = "Python",
            topic = "Loops",
            mastery = 50,
            sessionsCompleted = 2
        )
        coEvery { topicProgressDao.getProgress(1L, "Python", "Loops") } returns existing

        val slot = slot<TopicProgressEntity>()
        coEvery { topicProgressDao.upsertProgress(capture(slot)) } returns Unit

        // 0.7 * 50 + 0.3 * 80 = 35 + 24 = 59
        val newMastery = repository.updateMastery(1L, "Python", "Loops", 80.0)

        assertEquals(59, newMastery)
        assertEquals(59, slot.captured.mastery)
        assertEquals(3, slot.captured.sessionsCompleted)
    }

    @Test
    fun calculateStreak_returnsZeroWhenNoActivity() = runTest {
        coEvery { dailyActivityDao.getRecentActivities(1L, any()) } returns emptyList()

        val streak = repository.calculateStreak(1L, currentEpochDay = 20000L)
        assertEquals(0, streak)
    }

    @Test
    fun calculateStreak_returnsConsecutiveDaysIncludingToday() = runTest {
        val today = 20000L
        val activities = listOf(
            DailyActivityEntity(1L, today, xp = 50, minutes = 15),
            DailyActivityEntity(1L, today - 1, xp = 40, minutes = 10),
            DailyActivityEntity(1L, today - 2, xp = 30, minutes = 10)
        )
        coEvery { dailyActivityDao.getRecentActivities(1L, any()) } returns activities

        val streak = repository.calculateStreak(1L, currentEpochDay = today)
        assertEquals(3, streak)
    }

    @Test
    fun calculateStreak_preservesStreakIfStudiedYesterdayButNotToday() = runTest {
        val today = 20000L
        val activities = listOf(
            DailyActivityEntity(1L, today - 1, xp = 50, minutes = 15),
            DailyActivityEntity(1L, today - 2, xp = 40, minutes = 10)
        )
        coEvery { dailyActivityDao.getRecentActivities(1L, any()) } returns activities

        val streak = repository.calculateStreak(1L, currentEpochDay = today)
        assertEquals(2, streak)
    }

    @Test
    fun calculateStreak_resetsToZeroIfMissedYesterdayAndToday() = runTest {
        val today = 20000L
        val activities = listOf(
            DailyActivityEntity(1L, today - 2, xp = 50, minutes = 15),
            DailyActivityEntity(1L, today - 3, xp = 40, minutes = 10)
        )
        coEvery { dailyActivityDao.getRecentActivities(1L, any()) } returns activities

        val streak = repository.calculateStreak(1L, currentEpochDay = today)
        assertEquals(0, streak)
    }
}
