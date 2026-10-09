package com.mit.tushar_kaldate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "daily_activities",
    primaryKeys = ["userId", "dateEpochDay"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class DailyActivityEntity(
    val userId: Long,
    val dateEpochDay: Long,
    val xp: Int = 0,
    val minutes: Int = 0
)
