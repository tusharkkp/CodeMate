package com.mit.tushar_kaldate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mit.tushar_kaldate.data.model.EngineType
import com.mit.tushar_kaldate.data.model.LearnerLevel
import com.mit.tushar_kaldate.data.model.TutorMode

@Entity(
    tableName = "conversations",
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
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val title: String,
    val mode: TutorMode = TutorMode.CHAT,
    val language: String = "Python",
    val level: LearnerLevel = LearnerLevel.BEGINNER,
    val engine: EngineType = EngineType.CLOUD,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
