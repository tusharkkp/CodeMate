package com.mit.tushar_kaldate.data

import android.content.Context
import android.content.res.AssetManager
import com.mit.tushar_kaldate.data.curriculum.CurriculumData
import com.mit.tushar_kaldate.data.curriculum.CurriculumLanguage
import com.mit.tushar_kaldate.data.curriculum.CurriculumLoader
import com.mit.tushar_kaldate.data.curriculum.CurriculumTopic
import com.mit.tushar_kaldate.data.model.Difficulty
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class CurriculumLoaderTest {

    private val sampleJson = """
    {
      "languages": [
        {
          "id": "python",
          "name": "Python",
          "topics": [
            {
              "id": "python-loops",
              "title": "Loops & Iterations",
              "description": "Repeating tasks with for and while.",
              "difficulty": "EASY",
              "concepts": ["for loop", "while loop"]
            }
          ]
        },
        {
          "id": "java",
          "name": "Java",
          "topics": [
            {
              "id": "java-oop",
              "title": "OOP Basics",
              "description": "Classes and objects in Java.",
              "difficulty": "HARD",
              "concepts": ["class", "inheritance"]
            }
          ]
        }
      ]
    }
    """.trimIndent()

    @Test
    fun jsonDeserialization_parsesCurriculumDataCorrectly() {
        val json = Json { ignoreUnknownKeys = true }
        val data = json.decodeFromString<CurriculumData>(sampleJson)

        assertEquals(2, data.languages.size)
        val python = data.languages.first()
        assertEquals("python", python.id)
        assertEquals(1, python.topics.size)
        assertEquals("Loops & Iterations", python.topics.first().title)
        assertEquals(Difficulty.EASY, python.topics.first().difficulty)
    }

    @Test
    fun curriculumLoader_loadsAndFiltersTopics() {
        val mockContext: Context = mockk()
        val mockAssets: AssetManager = mockk()

        every { mockContext.assets } returns mockAssets
        every { mockAssets.open("curriculum.json") } answers {
            ByteArrayInputStream(sampleJson.toByteArray(Charsets.UTF_8))
        }

        val loader = CurriculumLoader(mockContext)
        val languages = loader.getLanguages()
        assertEquals(2, languages.size)

        val pythonTopics = loader.getTopicsForLanguage("python")
        assertEquals(1, pythonTopics.size)
        assertEquals("python-loops", pythonTopics.first().id)

        val searchResults = loader.searchTopics("inheritance")
        assertEquals(1, searchResults.size)
        assertEquals("java-oop", searchResults.first().id)
    }
}
