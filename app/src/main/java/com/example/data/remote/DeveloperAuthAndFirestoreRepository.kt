package com.example.data.remote

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlinx.coroutines.tasks.await
import java.util.Locale

const val AUTHORIZED_DEVELOPER_EMAIL = "alnaziali0@gmail.com"

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    // Never put account identifiers, email addresses, or tokens in Logcat.
    val currentUser = FirebaseAuth.getInstance().currentUser
    val diagnostic = JSONObject().apply {
        put("errorType", exception.javaClass.simpleName)
        put("operationType", operationType.value)
        put("path", path.orEmpty().take(160))
        put("isAuthenticated", currentUser != null)
        put("isEmailVerified", currentUser?.isEmailVerified == true)
        put("providerCount", currentUser?.providerData?.size ?: 0)
    }
    val jsonString = diagnostic.toString()
    Log.e("FirestoreError", "Firestore operation failed: $jsonString")
    return jsonString
}

@IgnoreExtraProperties
data class CloudAdminAnnouncement(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val badgeLabel: String = "إعلان",
    val actionUrl: String = "",
    val isActive: Boolean = true,
    val userId: String = "",
    val authorEmail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class AdminAnnouncementFirestoreRepository(private val db: FirebaseFirestore) {

    // CRITICAL: Always initialize with R.string.firestore_database_id (never zero-arg getInstance)
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth
        get() = Firebase.auth

    private fun requireAuthorizedDeveloper(): FirebaseUser {
        val user = auth.currentUser
            ?: throw IllegalStateException("يجب تسجيل الدخول بحساب Google المخصص للمطور أولاً.")
        val emailLower = user.email?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (emailLower != AUTHORIZED_DEVELOPER_EMAIL || !user.isEmailVerified) {
            throw SecurityException("هذا الإجراء متاح لحساب المطوّر المعتمد فقط.")
        }
        return user
    }

    fun observeAnnouncements(userId: String): Flow<List<CloudAdminAnnouncement>> {
        val collectionPath = COLLECTION_NAME
        return db.collection(collectionPath)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudAdminAnnouncement::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, collectionPath)
                }
                throw error
            }
    }

    suspend fun createAnnouncement(
        title: String,
        body: String,
        badgeLabel: String,
        actionUrl: String = ""
    ): Result<String> {
        val collectionPath = COLLECTION_NAME
        return try {
            val user = requireAuthorizedDeveloper()
            val cleanTitle = title.trim().take(160)
            val cleanBody = body.trim().take(1200)
            val cleanBadge = badgeLabel.trim().ifBlank { "إعلان إداري" }.take(60)
            val cleanUrl = actionUrl.trim().take(500)

            val docRef = db.collection(collectionPath).document()
            val payload = mutableMapOf<String, Any>(
                "title" to cleanTitle,
                "body" to cleanBody,
                "badgeLabel" to cleanBadge,
                "isActive" to true,
                "userId" to user.uid,
                "authorEmail" to AUTHORIZED_DEVELOPER_EMAIL,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanUrl.isNotEmpty()) {
                payload["actionUrl"] = cleanUrl
            }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, collectionPath)
            Result.failure(e)
        }
    }

    suspend fun updateAnnouncementActive(
        announcement: CloudAdminAnnouncement,
        active: Boolean
    ): Result<Unit> {
        require(announcement.id.isNotBlank()) { "Announcement ID must not be blank" }
        val docPath = "$COLLECTION_NAME/${announcement.id}"
        return try {
            requireAuthorizedDeveloper()
            db.collection(COLLECTION_NAME)
                .document(announcement.id)
                .update(
                    mapOf(
                        "isActive" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
        require(announcementId.isNotBlank()) { "Announcement ID must not be blank" }
        val docPath = "$COLLECTION_NAME/$announcementId"
        return try {
            requireAuthorizedDeveloper()
            db.collection(COLLECTION_NAME).document(announcementId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION_NAME = "admin_announcements"
    }
}

object DeveloperGoogleAuthHelper {
    private const val TAG = "Auth"

    /** Compose may expose a ContextThemeWrapper rather than the Activity itself. */
    private fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current != null) {
            if (current is Activity) return current
            current = (current as? ContextWrapper)?.baseContext
        }
        return null
    }

    fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        addAuthStateListener(listener)
        awaitClose { removeAuthStateListener(listener) }
    }

    fun isAuthorizedDeveloperUser(user: FirebaseUser?): Boolean {
        if (user == null) return false
        val email = user.email?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return email == AUTHORIZED_DEVELOPER_EMAIL && user.isEmailVerified
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAuthorizedSuccess: (FirebaseUser) -> Unit,
        onUnauthorizedAccount: (String) -> Unit,
        onUnauthenticated: () -> Unit
    ) {
        val existing = Firebase.auth.currentUser
        if (existing != null) {
            if (isAuthorizedDeveloperUser(existing)) {
                onAuthorizedSuccess(existing)
            } else {
                signOutDeveloper(credentialManager, scope) {
                    onUnauthorizedAccount("حساب غير مصرح")
                }
            }
            return
        }

        val activity = findActivity(context) ?: return
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onUnauthenticated()
            return
        }

        // Single credential option per request (keeps Credential Manager responsive on API 36)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    val signedIn = Firebase.auth.currentUser
                    if (isAuthorizedDeveloperUser(signedIn)) {
                        onAuthorizedSuccess(signedIn!!)
                    } else {
                        Firebase.auth.signOut()
                        try {
                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                        } catch (_: Exception) {
                        }
                        onUnauthorizedAccount("حساب غير مصرح")
                    }
                } else {
                    onUnauthenticated()
                }
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
    }

    /** Email/password auth is accepted only for the pre-provisioned, verified owner account.
     * Passwords are never persisted, logged, or embedded in the application package.
     */
    fun signInWithEmailPassword(
        email: String,
        password: CharSequence,
        scope: CoroutineScope,
        onAuthorizedSuccess: (FirebaseUser) -> Unit,
        onUnauthorizedAccount: () -> Unit,
        onAuthError: (String) -> Unit
    ) {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        if (cleanEmail != AUTHORIZED_DEVELOPER_EMAIL || password.isEmpty()) {
            onUnauthorizedAccount()
            return
        }
        scope.launch {
            try {
                Firebase.auth.signInWithEmailAndPassword(cleanEmail, password.toString()).await()
                val user = Firebase.auth.currentUser
                if (isAuthorizedDeveloperUser(user)) {
                    onAuthorizedSuccess(user!!)
                } else {
                    Firebase.auth.signOut()
                    onUnauthorizedAccount()
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                Log.e(TAG, "Email/password sign-in failed (${error.javaClass.simpleName})")
                onAuthError(
                    when {
                        error.message.orEmpty().contains("network", ignoreCase = true) -> "تعذر الاتصال بخدمة تسجيل الدخول. تحقق من الإنترنت."
                        error.message.orEmpty().contains("invalid", ignoreCase = true) || error.message.orEmpty().contains("credential", ignoreCase = true) -> "بيانات الدخول غير صحيحة أو الحساب غير مفعّل."
                        else -> "تعذر تسجيل الدخول. تحقق من تفعيل Email/Password في Firebase ومن توثيق البريد الإلكتروني."
                    }
                )
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAuthorizedSuccess: (FirebaseUser) -> Unit,
        onUnauthorizedAccount: (String) -> Unit,
        onAuthError: (String) -> Unit,
        onAuthCancelled: () -> Unit
    ) {
        val activity = findActivity(context)
        if (activity == null) {
            onAuthError("تعذر الوصول إلى نافذة التطبيق. أعد فتح لوحة المطوّر وحاول مجدداً.")
            return
        }

        val clientId = try {
            context.getString(R.string.default_web_client_id).trim().also {
                require(it.isNotEmpty()) { "Missing Web OAuth client ID" }
            }
        } catch (e: Exception) {
            onAuthError("إعداد Google غير مكتمل داخل Firebase. راجع ملف google-services.json ومعرّف Web OAuth.")
            return
        }

        scope.launch {
            try {
                val googleIdToken = requestGoogleIdToken(
                    activity = activity,
                    credentialManager = credentialManager,
                    clientId = clientId
                )
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                val signedIn = Firebase.auth.currentUser
                if (isAuthorizedDeveloperUser(signedIn)) {
                    onAuthorizedSuccess(signedIn!!)
                } else {
                    // Do not display or log the selected account's email.
                    Log.w(TAG, "Rejected sign-in: authenticated account is not the configured owner")
                    Firebase.auth.signOut()
                    try {
                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    } catch (_: Exception) {
                    }
                    onUnauthorizedAccount("حساب غير مصرح")
                }
            } catch (e: GetCredentialCancellationException) {
                onAuthCancelled()
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed (${e.javaClass.simpleName})")
                onAuthError(friendlyGoogleSignInError(e))
            }
        }
    }

    /**
     * Prefer the explicit Google button flow. If the provider cannot serve it on this device,
     * retry with the account chooser flow that accepts any device account; authorization is
     * still enforced by Firebase after receiving the ID token.
     */
    private suspend fun requestGoogleIdToken(
        activity: Activity,
        credentialManager: CredentialManager,
        clientId: String
    ): String {
        val buttonOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val buttonRequest = GetCredentialRequest.Builder()
            .addCredentialOption(buttonOption)
            .build()

        val buttonResult = try {
            credentialManager.getCredential(activity, buttonRequest)
        } catch (cancelled: GetCredentialCancellationException) {
            throw cancelled
        } catch (_: Exception) {
            val chooserOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()
            val chooserRequest = GetCredentialRequest.Builder()
                .addCredentialOption(chooserOption)
                .build()
            credentialManager.getCredential(activity, chooserRequest)
        }

        val credential = buttonResult.credential
        if (credential !is CustomCredential || credential.type != TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw IllegalStateException("Google provider returned an unsupported credential type")
        }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    private fun friendlyGoogleSignInError(error: Exception): String {
        val detail = (error.message ?: "").lowercase(Locale.ROOT)
        return when {
            "developer_error" in detail || "status code: 10" in detail || "12500" in detail ->
                "تعذر التحقق من إعدادات Google. تأكد من تفعيل Google في Firebase وإضافة بصمتي SHA-1 وSHA-256 لشهادة التطبيق الصحيحة."
            "network" in detail || "timeout" in detail || "unavailable" in detail ->
                "تعذر الاتصال بخدمة Google. تحقق من الإنترنت ثم أعد المحاولة."
            else ->
                "لم يكتمل تسجيل الدخول. تحقق من إعداد OAuth في Firebase ومن تحديث خدمات Google على الجهاز، ثم أعد المحاولة."
        }
    }

    fun signOutDeveloper(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onComplete: () -> Unit = {}
    ) {
        Firebase.auth.signOut()
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear credential state (${e.javaClass.simpleName})")
            } finally {
                onComplete()
            }
        }
    }
}
