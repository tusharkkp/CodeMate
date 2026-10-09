package com.mit.tushar_kaldate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mit.tushar_kaldate.data.local.entity.DailyActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordActivity(activity: DailyActivityEntity)

    @Query("SELECT * FROM daily_activities WHERE userId = :userId AND dateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getActivity(userId: Long, dateEpochDay: Long): DailyActivityEntity?

    @Query("SELECT * FROM daily_activities WHERE userId = :userId ORDER BY dateEpochDay DESC LIMIT :limitDays")
    fun observeRecentActivities(userId: Long, limitDays: Int = 30): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activities WHERE userId = :userId ORDER BY dateEpochDay DESC LIMIT :limitDays")
    suspend fun getRecentActivities(userId: Long, limitDays: Int = 30): List<DailyActivityEntity>

    @Query("SELECT SUM(xp) FROM daily_activities WHERE userId = :userId")
    suspend fun getTotalXp(userId: Long): Int?

    @Query("DELETE FROM daily_activities WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long): Int
}
