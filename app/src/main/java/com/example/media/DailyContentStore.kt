package com.example.media

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * مخزن خطة المحتوى اليومي (حديث اليوم / دعاء اليوم).
 *
 * يُكتب من ViewModel بعد قراءة المحتوى الموثّق من قاعدة البيانات المحلية،
 * ويُقرأ من مجدول الإشعارات عند بناء إنذارات كل صلاة، فيصل المحتوى
 * إلى المستخدم في إشعار مستقل بعد كل صلاة حتى لو كان التطبيق مغلقاً.
 *
 * الخطة تُحفظ في SharedPreferences لأنها صغيرة وتُقرأ بشكل متزامن من
 * BroadcastReceiver دون الحاجة لفتح قاعدة البيانات.
 */
object DailyContentStore {

    private const val PREFS = "alhadithi_daily_content_plan"
    private const val KEY_PLAN = "plan_json"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TYPE = "content_type"
    private const val KEY_PRAYERS = "prayer_times"

    data class ContentItem(
        val title: String,
        val body: String,
        val takhrij: String
    )

    /** يضيف محتوى اليوم لكل صلاة في نطاق الأيام upcomingDays. */
    fun putPlan(
        context: Context,
        items: Map<String, ContentItem>,
        contentType: String,
        enabledPrayers: Set<String>,
        enabled: Boolean
    ) {
        val array = JSONArray()
        items.forEach { (key, item) ->
            array.put(
                JSONObject().apply {
                    put("key", key)
                    put("title", item.title)
                    put("body", item.body)
                    put("takhrij", item.takhrij)
                }
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PLAN, array.toString())
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_TYPE, contentType)
            .putStringSet(KEY_PRAYERS, enabledPrayers)
            .apply()
    }

    fun planItem(context: Context, dateKey: String, prayerKey: String): ContentItem? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PLAN, null) ?: return null
        return runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                if (obj.optString("key") == "$dateKey|$prayerKey") {
                    return ContentItem(
                        title = obj.optString("title"),
                        body = obj.optString("body"),
                        takhrij = obj.optString("takhrij")
                    )
                }
            }
            null
        }.getOrNull()
    }

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun contentType(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TYPE, "hadith") ?: "hadith"

    fun enabledPrayers(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_PRAYERS, setOf("fajr", "dhuhr", "asr", "maghrib", "isha"))
            ?: setOf("fajr", "dhuhr", "asr", "maghrib", "isha")
}
