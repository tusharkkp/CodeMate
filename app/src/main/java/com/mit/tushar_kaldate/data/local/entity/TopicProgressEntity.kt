package com.mit.tushar_kaldate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "topic_progress",
    primaryKeys = ["userId", "language", "topic"],
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
data class TopicProgressEntity(
    val userId: Long,
    val language: String,
    val topic: String,
    val mastery: Int = 0,
    val sessionsCompleted: Int = 0,
    val lastStudiedAt: Long = System.currentTimeMillis()
)
