package com.example.base

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.net.HttpURLConnection
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
abstract class FirestoreEmulatorTestBase {

    protected lateinit var firestore: FirebaseFirestore
    protected lateinit var auth: FirebaseAuth
    protected lateinit var databaseId: String

    @Before
    open fun setUpFirebase() {
        // These tests only make sense against the local Firebase emulator. When it is not running
        // the test is skipped instead of failing, so the rest of the unit test suite stays green
        // on a machine without the emulator. See README for the emulator start command.
        assumeTrue(
            "The Firestore emulator must be listening on $EMULATOR_HOST:$FIRESTORE_PORT",
            isEmulatorReachable(EMULATOR_HOST, FIRESTORE_PORT)
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
        databaseId = if (resId != 0) context.getString(resId) else "ai-studio-android-0ccb0e51-02de-4d38-94ef-54f1255a8b5d"

        val app = if (FirebaseApp.getApps(context).isEmpty()) {
            val newApp = FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.alhadithi.iqshia")
                    .setProjectId(PROJECT_ID)
                    .setApiKey("fake-api-key-for-emulator")
                    .build()
            )
            val newFirestore = FirebaseFirestore.getInstance(newApp, databaseId)
            newFirestore.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
            newFirestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()

            val newAuth = FirebaseAuth.getInstance(newApp)
            newAuth.useEmulator(EMULATOR_HOST, AUTH_PORT)
            newApp
        } else {
            FirebaseApp.getInstance()
        }

        firestore = FirebaseFirestore.getInstance(app, databaseId)
        auth = FirebaseAuth.getInstance(app)
    }

    @After
    open fun tearDownFirebase() {
        // The setup may have been skipped because the emulator is not running, in that case there
        // is no authenticated session to close.
        if (this::auth.isInitialized) {
            auth.signOut()
        }
    }

    // Local unit test helper ONLY (under app/src/test/).
    protected suspend fun signInTestUser(
        email: String,
        emailVerified: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        withTimeout(AUTH_TIMEOUT_MS) {
            val result = try {
                auth.signInWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
            } catch (_: FirebaseAuthInvalidUserException) {
                auth.createUserWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
            } catch (_: Exception) {
                auth.createUserWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
            }
            val user = checkNotNull(result.user) { "User auth failed" }
            setEmulatorEmailVerified(user.uid, emailVerified)
            user.getIdToken(true).await()
            user.reload().await()
            user.uid
        }
    }

    private fun setEmulatorEmailVerified(uid: String, verified: Boolean) {
        val endpoint = URL(
            "http://$EMULATOR_HOST:$AUTH_PORT/identitytoolkit.googleapis.com/v1/projects/$PROJECT_ID/accounts:update"
        )
        val conn = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer owner")
        }
        val body = JSONObject().apply {
            put("localId", uid)
            put("emailVerified", verified)
        }.toString()
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        conn.responseCode
        conn.disconnect()
    }

    protected fun Throwable.unwrapFirestoreException(): FirebaseFirestoreException? {
        var current: Throwable? = this
        while (current != null) {
            if (current is FirebaseFirestoreException) return current
            current = current.cause
        }
        return null
    }

    private companion object {
        const val EMULATOR_HOST = "127.0.0.1"
        const val FIRESTORE_PORT = 8085
        const val AUTH_PORT = 9099
        const val PROJECT_ID = "demo-no-project"
        const val DEFAULT_PASSWORD = "password123"
        const val AUTH_TIMEOUT_MS = 5000L

        /** A short TCP probe so a missing emulator is detected in about a second. */
        fun isEmulatorReachable(host: String, port: Int): Boolean = try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(host, port), 1500)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
