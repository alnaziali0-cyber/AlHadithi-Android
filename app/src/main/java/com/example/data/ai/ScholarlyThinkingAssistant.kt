package com.example.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Deep-Thinking Islamic Scholarly Research & Curriculum Assistant powered by Firebase AI Logic.
 * Uses `gemini-4-argon` with `ThinkingLevel.HIGH` (no maxOutputTokens) and strict religious integrity guardrails.
 */
object ScholarlyThinkingAssistant {
    private const val TAG = "ScholarlyThinkingAI"
    private const val MODEL_NAME = "gemini-4-argon"

    private const val SYSTEM_GUARDRAILS = """
أنت الباحث العلمي المتعمق في تطبيق «الحديثي | AL-HADITHI» للموسوعة الإسلامية الشيعية الإمامية.
التزم التزاماً صارماً بالضوابط العلمية والشرعية الآتية:
1. لا تقم بتوليد آيات قرآنية من الذاكرة مع احتمال الخطأ، وإذا أشرت إلى آية فاذكر اسم السورة ورقم الآية بدقة واطلب من القارئ مراجعة نص المصحف العثماني المدمج في التطبيق.
2. لا تختلق أحاديث أو أرقام صفحات غير موثقة أبداً؛ اعزُ المعاني إلى أمهات المصادر المعتمدة (مثل الكافي للكليني، من لا يحضره الفقيه للصدوق، تهذيب الأحكام للطوسي، نهج البلاغة، الصحيفة السجادية، الميزان للطباطبائي، مجمع البيان للطبرسي).
3. عند وجود أكثر من رواية تاريخية (كروايتي ٧٥ يوماً و٩٥ يوماً في شهادة السيدة الزهراء عليها السلام)، اعرض الروايات بأمانة دون دمجها.
4. لا تُصدر فتاوى شرعية مستقلة؛ في المسائل الفقهية العملية اشرح القاعدة التعليمية العامة وفق الفقه الجعفري ثم وجّه السائل صراحةً إلى مراجعة الرسالة العملية لمرجع تقليده.
5. اكتب بلغة عربية فصيحة هادئة، مرتبة في نقاط واضحة، محترمة وموضوعية بعيداً عن التطويل المخلّ أو الجدل غير العلمي.
"""

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
            },
            systemInstruction = content {
                text(SYSTEM_GUARDRAILS)
            }
        )
    }

    suspend fun askScholarlyQuestion(
        question: String,
        localContextSnippet: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("يرجى كتابة السؤال العلمي أولاً."))
        }
        try {
            val promptText = buildString {
                if (localContextSnippet.isNotBlank()) {
                    appendLine("مقتطفات موثقة من قاعدة بيانات تطبيق الحديثي للاستئناس بها:")
                    appendLine(localContextSnippet)
                    appendLine("---")
                }
                appendLine("سؤال الباحث/المتعلم:")
                appendLine(trimmed)
            }
            val response = model.generateContent(promptText)
            val answer = response.text?.trim()
            if (!answer.isNullOrEmpty()) {
                Result.success(answer)
            } else {
                Result.failure(IllegalStateException("لم يتم استلام إجابة نصية من الباحث الذكي."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Thinking model inference error", e)
            Result.failure(e)
        }
    }
}
