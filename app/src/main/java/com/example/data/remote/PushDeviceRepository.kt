package com.example.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Push device registry.
 *
 * A device registers its FCM token in Firestore so a server-side notification
 * service can reach it. Registration is tied to the signed-in Firebase user id;
 * guests are not registered because there is no stable identity for them.
 *
 * Firestore rules (firestore.rules) enforce that a device may only write a
 * document whose userId equals its own uid.
 */
object PushDeviceRepository {

    private const val TAG = "PushDeviceRepo"
    const val COLLECTION_DEVICES = "notification_devices"

    private fun db(context: Context) =
        FirebaseFirestore.getInstance(context.getString(com.example.R.string.firestore_database_id))

    /**
     * Registers [token] for the currently signed-in user. Returns true when the
     * token was stored. Failure is reported, never thrown, so a signed-out or
     * offline device never crashes the app.
     */
    suspend fun registerToken(context: Context, token: String, platform: String = "android"): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        if (token.isBlank()) return false
        return try {
            db(context).collection(COLLECTION_DEVICES)
                .document(tokenHash(token))
                .set(
                    mapOf(
                        "userId" to user.uid,
                        "email" to (user.email ?: ""),
                        "token" to token,
                        "platform" to platform,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Device registration failed (${e.javaClass.simpleName})")
            false
        }
    }

    suspend fun unregisterToken(context: Context, token: String): Boolean {
        if (token.isBlank()) return false
        return try {
            db(context).collection(COLLECTION_DEVICES).document(tokenHash(token)).delete().await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Device removal failed (${e.javaClass.simpleName})")
            false
        }
    }

    /** Stores the result of a delivery attempt so the admin panel can show real outcomes. */
    suspend fun logDelivery(
        context: Context,
        kind: String,
        status: String,
        title: String? = null,
        body: String? = null,
        providerMessage: String? = null
    ): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        return try {
            db(context).collection("notification_log")
                .document()
                .set(
                    mapOf(
                        "userId" to user.uid,
                        "kind" to kind,
                        "status" to status,
                        "title" to (title ?: ""),
                        "body" to (body ?: ""),
                        "providerMessage" to (providerMessage ?: ""),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Delivery log write failed (${e.javaClass.simpleName})")
            false
        }
    }

    private fun tokenHash(token: String): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(48)
}
