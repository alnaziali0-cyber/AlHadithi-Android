package com.example.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedDataPart2EthicsTest {
    @Test
    fun `ethics curricula have the declared lessons and parseable quizzes`() {
        val courses = SeedDataPart2.ethicsCurricula()
        assertEquals(2, courses.size)
        assertTrue(courses.all { it.category == "الأخلاقيات" })

        courses.forEach { course ->
            val lessons = course.lessonsJson.split("||")
            val quizzes = course.quizJson.split("||")
            assertTrue("missing lessons: ${course.title}", lessons.isNotEmpty())
            assertTrue("missing quizzes: ${course.title}", quizzes.isNotEmpty())
            assertTrue(lessons.all { it.split("::").size == 4 })
            assertTrue(quizzes.all { question ->
                val parts = question.split("::")
                parts.size == 4 && parts[1].split(";;").size == 4 &&
                    (parts[2].toIntOrNull() ?: -1) in 0..3
            })
        }

        assertEquals(8, courses.first().lessonsJson.split("||").size)
        assertEquals(6, courses.last().lessonsJson.split("||").size)
    }
}
