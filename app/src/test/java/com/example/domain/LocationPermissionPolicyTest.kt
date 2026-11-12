package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPermissionPolicyTest {
    @Test
    fun grantedFinePermissionDoesNotShowRuntimePromptAgain() {
        assertEquals(
            LocationPermissionAction.USE_GRANTED_PERMISSION,
            LocationPermissionPolicy.nextAction(
                hasFinePermission = true,
                hasCoarsePermission = true,
                requestAlreadyShown = true,
                shouldShowSystemRationale = false
            )
        )
    }

    @Test
    fun firstExplicitLocationRequestShowsOneCombinedPermissionPrompt() {
        assertEquals(
            LocationPermissionAction.REQUEST_ONCE,
            LocationPermissionPolicy.nextAction(false, false, false, false)
        )
    }

    @Test
    fun deniedOrApproximatePermissionRoutesUserToSettingsInsteadOfPromptingAgain() {
        assertEquals(
            LocationPermissionAction.OPEN_APP_SETTINGS,
            LocationPermissionPolicy.nextAction(false, false, true, false)
        )
        assertEquals(
            LocationPermissionAction.OPEN_APP_SETTINGS,
            LocationPermissionPolicy.nextAction(false, true, false, false)
        )
        assertEquals(
            LocationPermissionAction.OPEN_APP_SETTINGS,
            LocationPermissionPolicy.nextAction(false, false, false, true)
        )
    }

    @Test
    fun locationFixMustBeRecentAndNotFromFuture() {
        val now = 1_800_000_000_000L
        assertTrue(LocationPermissionPolicy.isFreshFix(now, now))
        assertTrue(LocationPermissionPolicy.isFreshFix(now - 120_000L, now))
        assertFalse(LocationPermissionPolicy.isFreshFix(now - 120_001L, now))
        assertFalse(LocationPermissionPolicy.isFreshFix(now + 1L, now))
    }

    @Test
    fun locationFixMustHaveAtMostOneHundredMeterReportedAccuracy() {
        assertTrue(LocationPermissionPolicy.isAccurateFix(1f))
        assertTrue(LocationPermissionPolicy.isAccurateFix(100f))
        assertFalse(LocationPermissionPolicy.isAccurateFix(100.1f))
        assertFalse(LocationPermissionPolicy.isAccurateFix(0f))
        assertFalse(LocationPermissionPolicy.isAccurateFix(Float.NaN))
        assertFalse(LocationPermissionPolicy.isAccurateFix(Float.POSITIVE_INFINITY))
    }
}
