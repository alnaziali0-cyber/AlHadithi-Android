package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class AdminAnnouncementRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun verifiedAuthorizedDeveloper_canCreateObserveUpdateAndDeleteAnnouncement() = runBlocking {
        val ownerUid = signInTestUser(AUTHORIZED_DEVELOPER_EMAIL, emailVerified = true)
        val repository = AdminAnnouncementFirestoreRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createAnnouncement(
                title = "إعلان مجلس عاشوراء المركزي",
                body = "يُقام المجلس الحسيني يومياً الساعة الثامنة مساءً",
                badgeLabel = "مجلس حسيني",
                actionUrl = "https://alhadithi.iq"
            )
        }
        assertTrue("Expected owner create to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val docId = createResult.getOrThrow()

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAnnouncements(ownerUid).first { list -> list.any { it.id == docId } }
        }
        val createdItem = observed.first { it.id == docId }
        assertEquals("إعلان مجلس عاشوراء المركزي", createdItem.title)
        assertTrue(createdItem.isActive)

        val toggleResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateAnnouncementActive(createdItem, active = false)
        }
        assertTrue(toggleResult.isSuccess)

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteAnnouncement(docId)
        }
        assertTrue(deleteResult.isSuccess)
    }

    @Test
    fun otherVerifiedEmail_isRejectedByRulesAndClientGuard() = runBlocking {
        val intruderUid = signInTestUser("intruder_other@gmail.com", emailVerified = true)
        val randomId = "intruder_${UUID.randomUUID().toString().replace("-", "")}"

        // Attempt direct Firestore SDK write bypassing client email check to prove server rule rejects it
        var caught: FirebaseFirestoreException? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection(AdminAnnouncementFirestoreRepository.COLLECTION_NAME)
                    .document(randomId)
                    .set(
                        mapOf(
                            "title" to "اختراق",
                            "body" to "محاولة نشر بحساب آخر",
                            "badgeLabel" to "تنبيه",
                            "isActive" to true,
                            "userId" to intruderUid,
                            "authorEmail" to "intruder_other@gmail.com",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .await()
            }
        } catch (e: Exception) {
            caught = e.unwrapFirestoreException()
        }

        assertNotNull("Expected PERMISSION_DENIED for non-owner email", caught)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught?.code)
    }

    @Test
    fun unverifiedOwnerEmail_isRejectedByEmailSpoofingRule() = runBlocking {
        val unverifiedUid = signInTestUser(AUTHORIZED_DEVELOPER_EMAIL, emailVerified = false)
        val randomId = "spoof_${UUID.randomUUID().toString().replace("-", "")}"

        var caught: FirebaseFirestoreException? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection(AdminAnnouncementFirestoreRepository.COLLECTION_NAME)
                    .document(randomId)
                    .set(
                        mapOf(
                            "title" to "انتحال بريد",
                            "body" to "بريد غير مؤكد",
                            "badgeLabel" to "تنبيه",
                            "isActive" to true,
                            "userId" to unverifiedUid,
                            "authorEmail" to AUTHORIZED_DEVELOPER_EMAIL,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .await()
            }
        } catch (e: Exception) {
            caught = e.unwrapFirestoreException()
        }

        assertNotNull("Expected PERMISSION_DENIED when email_verified is false", caught)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught?.code)
    }

    @Test
    fun unauthenticatedCaller_observeAnnouncements_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = AdminAnnouncementFirestoreRepository(firestore)

        var caught: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeAnnouncements("any_uid").first()
            }
        } catch (e: Exception) {
            caught = e.unwrapFirestoreException()
        }

        assertFalse(auth.currentUser != null)
        assertNotNull("Expected nested FirebaseFirestoreException on unauthenticated query", caught)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught?.code)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
