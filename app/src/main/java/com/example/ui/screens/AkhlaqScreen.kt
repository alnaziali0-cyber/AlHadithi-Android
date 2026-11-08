package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AkhlaqCategoryEntity
import com.example.data.local.AkhlaqLessonEntity
import com.example.data.local.AkhlaqParsers
import com.example.data.local.AkhlaqProgressEntity
import com.example.data.local.AkhlaqQuizEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.HeritageGold

/**
 * قسم الأخلاقيات.
 *
 * يعرض خمسة عشر تصنيفاً، وكل تصنيف يجمع دروساً موثّقة: مقدمة، شرح مبسّط،
 * آية أو حديث موثّق مع تخريجه، أمثلة من الحياة اليومية، مواقف واقعية،
 * خطوات للتطبيق، أخطاء شائعة، واختباراً قصيراً مع متابعة للتقدّم.
 */
@Composable
fun AkhlaqScreen(
    categories: List<AkhlaqCategoryEntity>,
    lessons: List<AkhlaqLessonEntity>,
    progress: List<AkhlaqProgressEntity>,
    quizzesForLesson: (String) -> List<AkhlaqQuizEntity>,
    onMarkCompleted: (String, Int) -> Unit,
    onBack: () -> Unit
) {
    var openLessonId by remember { mutableStateOf<String?>(null) }
    val openLesson = lessons.firstOrNull { it.id == openLessonId }

    BackHandler(enabled = openLesson != null) { openLessonId = null }

    val completedIds = remember(progress) { progress.map { it.lessonId }.toSet() }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "الأخلاقيات",
            subtitle = "${lessons.size} درساً في ${categories.size} تصنيفاً",
            onBack = { if (openLesson != null) openLessonId = null else onBack() }
        )

        if (openLesson == null) {
            AkhlaqCategoryList(
                categories = categories,
                lessons = lessons,
                completedIds = completedIds,
                onOpenLesson = { openLessonId = it }
            )
        } else {
            AkhlaqLessonDetail(
                lesson = openLesson,
                quizzes = quizzesForLesson(openLesson.id),
                completed = openLesson.id in completedIds,
                onMarkCompleted = { score -> onMarkCompleted(openLesson.id, score) },
                onBack = { openLessonId = null }
            )
        }
    }
}

@Composable
private fun AkhlaqCategoryList(
    categories: List<AkhlaqCategoryEntity>,
    lessons: List<AkhlaqLessonEntity>,
    completedIds: Set<String>,
    onOpenLesson: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            val categoryLessons = lessons.filter { it.categoryId == category.id }
            val done = categoryLessons.count { it.id in completedIds }
            GlassCard(modifier = Modifier.fillMaxWidth().testTag("akhlaq_category_${category.id}")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (done == categoryLessons.size && categoryLessons.isNotEmpty()) {
                                "مكتمل · ${categoryLessons.size} دروس"
                            } else {
                                "${categoryLessons.size} دروس · أُنجز $done"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                categoryLessons.forEach { lesson ->
                    Spacer(modifier = Modifier.height(8.dp))
                    AkhlaqLessonRow(
                        lesson = lesson,
                        completed = lesson.id in completedIds,
                        onClick = { onOpenLesson(lesson.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AkhlaqLessonRow(
    lesson: AkhlaqLessonEntity,
    completed: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .testTag("akhlaq_lesson_${lesson.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AkhlaqLessonDetail(
    lesson: AkhlaqLessonEntity,
    quizzes: List<AkhlaqQuizEntity>,
    completed: Boolean,
    onMarkCompleted: (Int) -> Unit,
    onBack: () -> Unit
) {
    val ayat = remember(lesson.id) { AkhlaqParsers.ayat(lesson.evidenceAyat) }
    val hadith = remember(lesson.id) { AkhlaqParsers.hadith(lesson.evidenceHadith) }
    val examples = remember(lesson.id) { AkhlaqParsers.stringList(lesson.examples) }
    val scenarios = remember(lesson.id) { AkhlaqParsers.scenarios(lesson.scenarios) }
    val applySteps = remember(lesson.id) { AkhlaqParsers.stringList(lesson.applySteps) }
    val mistakes = remember(lesson.id) { AkhlaqParsers.stringList(lesson.mistakes) }
    var selectedAnswers by remember(lesson.id) { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (lesson.intro.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = lesson.intro,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (lesson.body.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = lesson.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (ayat.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeaderRow(title = "الدليل من القرآن الكريم")
                    ayat.forEach { ayah ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = ayah.text,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = ayah.reference,
                            style = MaterialTheme.typography.labelSmall,
                            color = HeritageGold
                        )
                    }
                }
            }
        }

        if (hadith != null) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeaderRow(title = "الدليل من السنة")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = hadith.text, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = hadith.takhrij,
                        style = MaterialTheme.typography.labelSmall,
                        color = HeritageGold
                    )
                }
            }
        }

        if (examples.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeaderRow(title = "أمثلة من الحياة اليومية")
                    examples.forEach { example ->
                        Spacer(modifier = Modifier.height(6.dp))
                        BulletLine(text = example)
                    }
                }
            }
        }

        if (scenarios.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeaderRow(title = "موقف واقعي: ماذا ستفعل؟")
                    scenarios.forEach { scenario ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = scenario.situation,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        scenario.options.forEachIndexed { index, option ->
                            Spacer(modifier = Modifier.height(6.dp))
                            val chosen = selectedAnswers[scenario.situation] == index
                            val answered = selectedAnswers.containsKey(scenario.situation)
                            val isCorrect = index == scenario.correctIndex
                            val container = when {
                                !answered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                isCorrect -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                chosen -> MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = container,
                                border = BorderStroke(
                                    1.dp,
                                    if (answered && isCorrect) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedAnswers = selectedAnswers + (scenario.situation to index)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(text = option, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        if (applySteps.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeaderRow(title = "كيف تطبّق هذا الخلق؟")
                    applySteps.forEach { step ->
                        Spacer(modifier = Modifier.height(6.dp))
                        BulletLine(text = step)
                    }
                }
            }
        }

        if (mistakes.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeaderRow(title = "أخطاء يجب تجنبها")
                    mistakes.forEach { mistake ->
                        Spacer(modifier = Modifier.height(6.dp))
                        BulletLine(text = mistake)
                    }
                }
            }
        }

        if (quizzes.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeaderRow(title = "اختبار قصير")
                    quizzes.forEach { quiz ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Quiz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = quiz.question,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        val options = remember(quiz.id) { AkhlaqParsers.stringList(quiz.options) }
                        options.forEachIndexed { index, option ->
                            Spacer(modifier = Modifier.height(6.dp))
                            val chosen = selectedAnswers[quiz.id] == index
                            val answered = selectedAnswers.containsKey(quiz.id)
                            val isCorrect = index == quiz.answerIndex
                            val container = when {
                                !answered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                isCorrect -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                chosen -> MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = container,
                                border = BorderStroke(
                                    1.dp,
                                    if (answered && isCorrect) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAnswers = selectedAnswers + (quiz.id to index) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(text = option, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        AnimatedVisibility(visible = selectedAnswers.containsKey(quiz.id)) {
                            Column {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (selectedAnswers[quiz.id] == quiz.answerIndex) {
                                        "إجابة صحيحة. ${quiz.explanation}"
                                    } else {
                                        quiz.explanation
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (lesson.summary.isNotBlank()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeaderRow(title = "خلاصة عملية")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = lesson.summary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    val total = quizzes.size
                    val correct = quizzes.count { selectedAnswers[it.id] == it.answerIndex }
                    val score = if (total == 0) 100 else (correct * 100) / total
                    onMarkCompleted(score)
                }) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (completed) "تم الإنجاز · حدّث التقدّم" else "تمّت قراءة الدرس")
                }
            }
        }
    }
}

@Composable
private fun BulletLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Outlined.Lightbulb,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}
