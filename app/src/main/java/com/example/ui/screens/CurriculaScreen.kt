package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CurriculumCourseEntity
import com.example.data.local.LessonProgressEntity
import com.example.data.local.QuizAttemptEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.SourceCitationBadge
import com.example.ui.components.SubScreenTopBar

private data class ParsedLesson(
    val unitTitle: String,
    val lessonTitle: String,
    val body: String,
    val summary: String
)

private data class ParsedQuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

private fun parseCourseLessons(raw: String): List<ParsedLesson> =
    raw.split("||").mapNotNull { chunk ->
        val parts = chunk.split("::")
        if (parts.size < 4) null
        else ParsedLesson(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim())
    }

private fun parseCourseQuizzes(raw: String): List<ParsedQuizQuestion> =
    raw.split("||").mapNotNull { chunk ->
        val parts = chunk.split("::")
        if (parts.size < 4) null
        else {
            val opts = parts[1].split(";;").map { it.trim() }
            val cIdx = parts[2].trim().toIntOrNull() ?: 0
            ParsedQuizQuestion(parts[0].trim(), opts, cIdx, parts[3].trim())
        }
    }

@Composable
fun CurriculaScreen(
    courses: List<CurriculumCourseEntity>,
    lessonProgress: List<LessonProgressEntity>,
    quizAttempts: List<QuizAttemptEntity>,
    initialCategory: String? = null,
    onBack: () -> Unit,
    onMarkLessonCompleted: (Long, Int) -> Unit,
    onSaveQuizAttempt: (Long, String, Int, Int) -> Unit,
    onOpenAiScholar: () -> Unit
) {
    var activeCourse by remember { mutableStateOf<CurriculumCourseEntity?>(null) }
    var takingQuizForCourse by remember { mutableStateOf<CurriculumCourseEntity?>(null) }

    if (takingQuizForCourse != null) {
        val course = takingQuizForCourse!!
        val questions = remember(course.quizJson) { parseCourseQuizzes(course.quizJson) }
        val selectedAnswers = remember(course.id) { mutableStateMapOf<Int, Int>() }
        var submitted by remember(course.id) { mutableStateOf(false) }

        BackHandler { takingQuizForCourse = null }

        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = "اختبار: ${course.title}",
                subtitle = "${questions.size} أسئلة موضوعية مع تعليل الإجابات",
                onBack = { takingQuizForCourse = null }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (submitted) {
                    val score = questions.indices.count { selectedAnswers[it] == questions[it].correctIndex }
                    item {
                        GlassCard(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "النتيجة النهائية: $score من ${questions.size}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "حُفظت نتيجتك في سجل الاختبارات المحلي. راجع تعليلات الإجابات أدناه.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                items(questions.size) { qIdx ->
                    val q = questions[qIdx]
                    GlassCard {
                        Text(
                            text = "س${qIdx + 1}: ${q.question}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        q.options.forEachIndexed { optIdx, optText ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = selectedAnswers[qIdx] == optIdx,
                                    enabled = !submitted,
                                    onClick = { selectedAnswers[qIdx] = optIdx }
                                )
                                Text(
                                    text = optText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        submitted && optIdx == q.correctIndex -> MaterialTheme.colorScheme.primary
                                        submitted && selectedAnswers[qIdx] == optIdx -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (submitted && optIdx == q.correctIndex) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        if (submitted) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "التعليل العلمي: ${q.explanation}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    if (!submitted) {
                        Button(
                            onClick = {
                                val score = questions.indices.count { selectedAnswers[it] == questions[it].correctIndex }
                                submitted = true
                                onSaveQuizAttempt(course.id, course.title, score, questions.size)
                            },
                            enabled = selectedAnswers.size == questions.size,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تصحيح الاختبار وحفظ النتيجة")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { takingQuizForCourse = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("العودة إلى المنهج")
                        }
                    }
                }
            }
        }
        return
    }

    if (activeCourse != null) {
        val course = activeCourse!!
        val lessons = remember(course.lessonsJson) { parseCourseLessons(course.lessonsJson) }
        var activeLessonIdx by remember(course.id) { mutableIntStateOf(0) }
        val lesson = lessons.getOrElse(activeLessonIdx) {
            ParsedLesson("الوحدة", course.title, course.summary, "")
        }
        val completedForCourse = lessonProgress.filter { it.courseId == course.id }.map { it.lessonIndex }.toSet()

        BackHandler { activeCourse = null }

        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = course.title,
                subtitle = "${course.category} · ${course.levelLabel}",
                onBack = { activeCourse = null }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    GlassCard {
                        Text(
                            text = lesson.unitTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "الدرس ${activeLessonIdx + 1} من ${lessons.size}: ${lesson.lessonTitle}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = lesson.body,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (lesson.summary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            ) {
                                Text(
                                    text = "خلاصة الدرس: ${lesson.summary}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        SourceCitationBadge(
                            attribution = "المصدر التعليمي المعتمد",
                            sourceBook = course.sourceReference
                        )
                    }
                }

                item {
                    val isDone = activeLessonIdx in completedForCourse
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onMarkLessonCompleted(course.id, activeLessonIdx)
                                if (activeLessonIdx < lessons.lastIndex) {
                                    activeLessonIdx++
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isDone) "مكتمل · الدرس التالي" else "إتمام الدرس والمتابعة")
                        }
                        OutlinedButton(
                            onClick = { takingQuizForCourse = course }
                        ) {
                            Icon(Icons.Outlined.Quiz, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الاختبار")
                        }
                    }
                }
            }
        }
        return
    }

    BackHandler { onBack() }

    val displayedCourses = remember(courses, initialCategory) {
        if (initialCategory == null) courses else courses.filter { it.category == initialCategory }
    }
    val displayedCourseIds = remember(displayedCourses) { displayedCourses.map { it.id }.toSet() }
    val displayedProgress = remember(lessonProgress, displayedCourseIds) {
        lessonProgress.filter { it.courseId in displayedCourseIds }
    }
    val displayedAttempts = remember(quizAttempts, displayedCourseIds) {
        quizAttempts.filter { it.courseId in displayedCourseIds }
    }
    val totalLessonsCount = remember(displayedCourses) {
        displayedCourses.sumOf { parseCourseLessons(it.lessonsJson).size }
    }
    val completedTotal = displayedProgress.size

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = initialCategory ?: "المناهج والعلوم الإسلامية",
            subtitle = if (initialCategory == "الأخلاقيات") {
                "دروس عملية في الحقوق والسلوك والنزاهة الرقمية مع اختبارات قصيرة وتقدم محفوظ"
            } else {
                "دروس منهجية واختبارات قصيرة ونسبة إنجاز محفوظة على جهازك"
            },
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("curricula_courses_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("تقدمك التعليمي الإجمالي", fontWeight = FontWeight.Bold)
                            Text(
                                text = "$completedTotal من $totalLessonsCount درساً مكتمل",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        OutlinedButton(onClick = onOpenAiScholar) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اسأل الباحث الذكي")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (totalLessonsCount == 0) 0f
                            else (completedTotal.toFloat() / totalLessonsCount.toFloat()).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            items(displayedCourses, key = { it.id }) { course ->
                val cLessons = parseCourseLessons(course.lessonsJson).size.coerceAtLeast(1)
                val cDone = displayedProgress.count { it.courseId == course.id }
                val pct = ((cDone.toFloat() / cLessons.toFloat()) * 100f).toInt().coerceIn(0, 100)

                GlassCard(
                    onClick = { activeCourse = course }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = course.category,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$pct% مكتمل",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = course.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { activeCourse = course }) {
                            Text("فتح الدروس ($cLessons)")
                        }
                        OutlinedButton(onClick = { takingQuizForCourse = course }) {
                            Text("بدء الاختبار")
                        }
                    }
                }
            }

            if (quizAttempts.isNotEmpty()) {
                item {
                    Text(
                        text = "سجل نتائج الاختبارات السابقة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(displayedAttempts.take(8), key = { it.id }) { attempt ->
                    GlassCard(cornerRadius = 16.dp, contentPadding = PaddingValues(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(attempt.courseTitle, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${attempt.score} / ${attempt.totalQuestions}",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (displayedCourses.isEmpty()) {
                item {
                    GlassCard {
                        Text(
                            text = "لا توجد دروس منشورة في هذا القسم بعد.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Text(
                    text = "تنبيه: الدروس مداخل تعليمية عامة مع ذكر مصادرها. المسائل الفقهية العملية يُراجع تفصيلها في الرسالة العملية لمرجع التقليد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
