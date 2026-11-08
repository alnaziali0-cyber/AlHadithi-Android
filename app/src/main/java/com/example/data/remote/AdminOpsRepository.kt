package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** سجل عملية إدارية (من لوحة المطور). */
data class AuditEntry(
    val id: String,
    val action: String,
    val actorEmail: String,
    val entity: String,
    val createdAt: Long?,
    val meta: String
)

/** جهاز مسجّل لاستقبال الإشعارات. */
data class RegisteredDevice(
    val id: String,
    val userId: String,
    val platform: String,
    val updatedAt: Long?
)

/** سجل إرسال إشعار. */
data class NotificationLogEntry(
    val id: String,
    val kind: String,
    val status: String,
    val title: String,
    val createdAt: Long?,
    val providerMessage: String
)

/**
 * قراءة بيانات الإدارة من Firestore.
 *
 * كل هذه المجموعات محمية بقواعد firestore.rules بحيث لا يقرأها إلا حساب
 * المطوّر المعتمد (alnaziali0@gmail.com).
 */
object AdminOpsRepository {

    private const val TAG = "AdminOps"

    private fun db(context: Context) =
        FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))

    fun observeDevices(context: Context): Flow<List<RegisteredDevice>> =
        db(context).collection("notification_devices")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(200)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    RegisteredDevice(
                        id = doc.id,
                        userId = doc.getString("userId").orEmpty(),
                        platform = doc.getString("platform") ?: "android",
                        updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time
                    )
                }
            }

    fun observeNotificationLog(context: Context): Flow<List<NotificationLogEntry>> =
        db(context).collection("notification_log")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    NotificationLogEntry(
                        id = doc.id,
                        kind = doc.getString("kind").orEmpty(),
                        status = doc.getString("status").orEmpty(),
                        title = doc.getString("title").orEmpty(),
                        createdAt = doc.getTimestamp("createdAt")?.toDate()?.time,
                        providerMessage = doc.getString("providerMessage").orEmpty()
                    )
                }
            }

    fun observeAuditLog(context: Context): Flow<List<AuditEntry>> =
        db(context).collection("admin_audit_log")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(150)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    AuditEntry(
                        id = doc.id,
                        action = doc.getString("action").orEmpty(),
                        actorEmail = doc.getString("actorEmail").orEmpty(),
                        entity = doc.getString("entity").orEmpty(),
                        createdAt = doc.getTimestamp("createdAt")?.toDate()?.time,
                        meta = doc.getString("meta").orEmpty()
                    )
                }
            }

    /** يسجّل عملية إدارية في السجل (يستدعيه الخادم بعد تنفيذ العملية). */
    suspend fun writeAudit(
        context: Context,
        action: String,
        entity: String,
        actorEmail: String,
        meta: String = ""
    ): Boolean = try {
        db(context).collection("admin_audit_log").add(
            mapOf(
                "action" to action,
                "entity" to entity,
                "actorEmail" to actorEmail,
                "meta" to meta,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        )
        true
    } catch (e: Exception) {
        Log.w(TAG, "audit write failed (${e.javaClass.simpleName})")
        false
    }
}
