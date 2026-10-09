package com.mit.tushar_kaldate.data.curriculum

import com.mit.tushar_kaldate.data.model.Difficulty
import kotlinx.serialization.Serializable

@Serializable
data class CurriculumData(
    val languages: List<CurriculumLanguage> = emptyList()
)

@Serializable
data class CurriculumLanguage(
    val id: String,
    val name: String,
    val topics: List<CurriculumTopic> = emptyList()
)

@Serializable
data class CurriculumTopic(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: Difficulty = Difficulty.EASY,
    val concepts: List<String> = emptyList()
)
