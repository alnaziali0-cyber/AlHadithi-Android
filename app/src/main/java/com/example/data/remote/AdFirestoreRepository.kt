package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

/**
 * إعلانات التطبيق (Ads).
 *
 * دورة حياة الإعلان:
 *  ١) يُنشئ المطور الإعلان في مجموعة `ads` (نسخة التحرير — للمطوّر وحده).
 *  ٢) عند النشر تُنسخ نسخة إلى `published_ads` وهي ما يقرأه كل المستخدمين.
 *  ٣) يمكن جدولة الإعلان أو إيقافه أو حذفه من لوحة المطور.
 *  ٤) عند اختيار "إرسال إشعار" تُسجَّل Devices في `notification_devices` ويقوم
 *     خادم الإشعارات (FCM) بالتوصيل.
 *
 * كل عمليات الكتابة محصورة في حساب المطوّر المعتمد عبر قواعد Firestore
 * (firestore.rules) — لا يستطيع أي مستخدم آخر النشر أو التعديل.
 */
data class AdEntity(
    val id: String = "",
    val title: String = "",
    val text: String = "",
    val shortDesc: String = "",
    val link: String = "",
    val ctaLabel: String = "",
    val imagePath: String = "",
    val imageMime: String = "",
    val imageBytes: Long = 0,
    val startAt: Long? = null,
    val endAt: Long? = null,
    val status: String = "draft",
    val target: String = "all",
    val targetRole: String = "",
    val sendPush: Boolean = false,
    val pushedAt: Long? = null,
    val createdAt: Long? = null
) {
    val isLive: Boolean
        get() = status == "active" &&
            (startAt == null || startAt <= System.currentTimeMillis()) &&
            (endAt == null || endAt >= System.currentTimeMillis())
}

object AdFirestoreRepository {

    private const val TAG = "AdRepo"
    const val COLLECTION_ADS = "ads"
    const val COLLECTION_PUBLISHED = "published_ads"
    const val STORAGE_FOLDER = "ads"

    private fun db(context: Context) =
        FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))

    private fun storage(context: Context) =
        FirebaseStorage.getInstance(context.getString(R.string.firebase_storage_bucket_id))

    /** الإعلانات المنشورة التي يراها كل المستخدمين. */
    fun observePublishedAds(context: Context) =
        db(context).collection(COLLECTION_PUBLISHED)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.toAd() } }

    /** كل الإعلانات — للمطوّر فقط. */
    fun observeAllAds(context: Context) =
        db(context).collection(COLLECTION_ADS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.toAd() } }

    /** ينشئ إعلاناً جديداً في مجموعة التحرير. */
    suspend fun createAd(context: Context, ad: AdEntity): Result<String> = try {
        val ref = db(context).collection(COLLECTION_ADS).document()
        ref.set(
            mapOf(
                "title" to ad.title.trim(),
                "text" to ad.text.trim(),
                "shortDesc" to ad.shortDesc.trim(),
                "link" to ad.link.trim(),
                "ctaLabel" to ad.ctaLabel.trim(),
                "imagePath" to ad.imagePath,
                "imageMime" to ad.imageMime,
                "imageBytes" to ad.imageBytes,
                "startAt" to ad.startAt?.let { com.google.firebase.Timestamp(it, 0) },
                "endAt" to ad.endAt?.let { com.google.firebase.Timestamp(it, 0) },
                "status" to ad.status,
                "target" to ad.target,
                "targetRole" to ad.targetRole,
                "sendPush" to ad.sendPush,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(ref.id)
    } catch (e: Exception) {
        Log.w(TAG, "createAd failed (${e.javaClass.simpleName})")
        Result.failure(e)
    }

    suspend fun updateAd(context: Context, ad: AdEntity): Result<Unit> = try {
        db(context).collection(COLLECTION_ADS).document(ad.id)
            .set(
                mapOf(
                    "title" to ad.title.trim(),
                    "text" to ad.text.trim(),
                    "shortDesc" to ad.shortDesc.trim(),
                    "link" to ad.link.trim(),
                    "ctaLabel" to ad.ctaLabel.trim(),
                    "imagePath" to ad.imagePath,
                    "imageMime" to ad.imageMime,
                    "imageBytes" to ad.imageBytes,
                    "startAt" to ad.startAt?.let { com.google.firebase.Timestamp(it, 0) },
                    "endAt" to ad.endAt?.let { com.google.firebase.Timestamp(it, 0) },
                    "status" to ad.status,
                    "target" to ad.target,
                    "targetRole" to ad.targetRole,
                    "sendPush" to ad.sendPush,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.w(TAG, "updateAd failed (${e.javaClass.simpleName})")
        Result.failure(e)
    }

    /** ينشر الإعلان: ينسخه إلى published_ads ليظهر لكل المستخدمين. */
    suspend fun publishAd(context: Context, ad: AdEntity): Result<Unit> = try {
        val payload = mapOf(
            "title" to ad.title.trim(),
            "text" to ad.text.trim(),
            "shortDesc" to ad.shortDesc.trim(),
            "link" to ad.link.trim(),
            "ctaLabel" to ad.ctaLabel.trim(),
            "imagePath" to ad.imagePath,
            "imageMime" to ad.imageMime,
            "imageBytes" to ad.imageBytes,
            "startAt" to ad.startAt?.let { com.google.firebase.Timestamp(it, 0) },
            "endAt" to ad.endAt?.let { com.google.firebase.Timestamp(it, 0) },
            "status" to "active",
            "target" to ad.target,
            "targetRole" to ad.targetRole,
            "sendPush" to ad.sendPush,
            "publishedAt" to FieldValue.serverTimestamp()
        )
        db(context).collection(COLLECTION_PUBLISHED).document(ad.id).set(payload).await()
        db(context).collection(COLLECTION_ADS).document(ad.id)
            .set(mapOf("status" to "active", "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.w(TAG, "publishAd failed (${e.javaClass.simpleName})")
        Result.failure(e)
    }

    /** يوقف الإعلان ويخفي نسخة النشر. */
    suspend fun pauseAd(context: Context, adId: String): Result<Unit> = try {
        db(context).collection(COLLECTION_ADS).document(adId)
            .set(mapOf("status" to "paused", "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
            .await()
        db(context).collection(COLLECTION_PUBLISHED).document(adId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteAd(context: Context, adId: String): Result<Unit> = try {
        db(context).collection(COLLECTION_ADS).document(adId).delete().await()
        db(context).collection(COLLECTION_PUBLISHED).document(adId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * يرفع صورة الإعلان من الهاتف إلى Firebase Storage بعد ضغطها وتحسينها،
     * ويعيد المسار العام. الملفات لا تُخزَّن داخل قاعدة البيانات.
     */
    suspend fun uploadAdImage(context: Context, adId: String, uri: Uri): Result<String> {
        return try {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        if (!mime.startsWith("image/")) {
            return Result.failure(IllegalArgumentException("نوع الملف ليس صورة"))
        }
        val original = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return Result.failure(IllegalStateException("تعذّرت قراءة الصورة"))
        if (original.size > MAX_UPLOAD_BYTES) {
            return Result.failure(IllegalArgumentException("حجم الصورة أكبر من الحد المسموح"))
        }

        // ضغط وتحسين الصورة قبل الرفع لتقليل الحجم واستهلاك البيانات.
        val compressed = android.graphics.BitmapFactory
            .decodeByteArray(original, 0, original.size)
            ?.let { bitmap ->
                val out = ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.WEBP, 85, out)
                bitmap.recycle()
                out.toByteArray()
            } ?: original

        val extension = if (mime.contains("png")) "png" else "jpg"
        val ref = storage(context).reference.child("$STORAGE_FOLDER/$adId/image.$extension")
        val metadata = StorageMetadata.Builder()
            .setContentType(mime)
            .setCustomMetadata("uploadedBy", "alhadithi-developer-panel")
            .build()
        ref.putBytes(compressed, metadata).await()
        Result.success(ref.downloadUrl.await().toString())
        } catch (e: Exception) {
            Log.w(TAG, "uploadAdImage failed (${e.javaClass.simpleName})")
            Result.failure(e)
        }
    }

    private const val MAX_UPLOAD_BYTES = 8 * 1024 * 1024

    private fun com.google.firebase.firestore.DocumentSnapshot.toAd(): AdEntity = AdEntity(
        id = id,
        title = getString("title").orEmpty(),
        text = getString("text").orEmpty(),
        shortDesc = getString("shortDesc").orEmpty(),
        link = getString("link").orEmpty(),
        ctaLabel = getString("ctaLabel").orEmpty(),
        imagePath = getString("imagePath").orEmpty(),
        imageMime = getString("imageMime").orEmpty(),
        imageBytes = (getLong("imageBytes") ?: 0L),
        startAt = getTimestamp("startAt")?.toDate()?.time,
        endAt = getTimestamp("endAt")?.toDate()?.time,
        status = getString("status") ?: "draft",
        target = getString("target") ?: "all",
        targetRole = getString("targetRole").orEmpty(),
        sendPush = getBoolean("sendPush") ?: false,
        pushedAt = getTimestamp("pushedAt")?.toDate()?.time,
        createdAt = getTimestamp("createdAt")?.toDate()?.time
    )
}
