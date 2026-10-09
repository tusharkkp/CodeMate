package com.mit.tushar_kaldate.data.local

import androidx.room.TypeConverter
import com.mit.tushar_kaldate.data.model.Difficulty
import com.mit.tushar_kaldate.data.model.EngineType
import com.mit.tushar_kaldate.data.model.LearnerLevel
import com.mit.tushar_kaldate.data.model.MessageRole
import com.mit.tushar_kaldate.data.model.TutorMode

class Converters {

    @TypeConverter
    fun fromTutorMode(mode: TutorMode?): String? = mode?.name

    @TypeConverter
    fun toTutorMode(value: String?): TutorMode? = value?.let {
        try {
            TutorMode.valueOf(it)
        } catch (e: IllegalArgumentException) {
            TutorMode.CHAT
        }
    }

    @TypeConverter
    fun fromMessageRole(role: MessageRole?): String? = role?.name

    @TypeConverter
    fun toMessageRole(value: String?): MessageRole? = value?.let {
        try {
            MessageRole.valueOf(it)
        } catch (e: IllegalArgumentException) {
            MessageRole.USER
        }
    }

    @TypeConverter
    fun fromLearnerLevel(level: LearnerLevel?): String? = level?.name

    @TypeConverter
    fun toLearnerLevel(value: String?): LearnerLevel? = value?.let {
        try {
            LearnerLevel.valueOf(it)
        } catch (e: IllegalArgumentException) {
            LearnerLevel.BEGINNER
        }
    }

    @TypeConverter
    fun fromEngineType(type: EngineType?): String? = type?.name

    @TypeConverter
    fun toEngineType(value: String?): EngineType? = value?.let {
        try {
            EngineType.valueOf(it)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    @TypeConverter
    fun fromDifficulty(diff: Difficulty?): String? = diff?.name

    @TypeConverter
    fun toDifficulty(value: String?): Difficulty? = value?.let {
        try {
            Difficulty.valueOf(it)
        } catch (e: IllegalArgumentException) {
            Difficulty.EASY
        }
    }
}
