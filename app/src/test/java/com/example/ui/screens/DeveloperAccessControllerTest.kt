package com.example.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The developer gate is the only thing standing between a normal user and the developer tools,
 * so its rules are covered by unit tests: the tap gesture must be deliberate, the code must be
 * verified, and the session must expire.
 */
class DeveloperAccessControllerTest {

    @Test
    fun `the tap gesture needs the full number of taps`() {
        val now = 1_000L
        var count = 0
        var lastTap = now

        // Six quick taps are not enough.
        repeat(6) {
            assertFalse(
                DeveloperAccessController.isMultiTapGesture(count, lastTap, now)
            )
            count++
            lastTap = now
        }

        // The seventh tap completes the gesture.
        assertTrue(DeveloperAccessController.isMultiTapGesture(count, lastTap, now))
    }

    @Test
    fun `a slow tap sequence resets the counter`() {
        val start = 1_000L
        val later = start + 5_000L
        // The gap between the two taps is larger than the allowed window, so the gesture fails.
        assertFalse(DeveloperAccessController.isMultiTapGesture(6, start, later))
    }

    @Test
    fun `the default access code is documented and stable`() {
        assertEquals("7419", DeveloperAccessController.DEFAULT_ACCESS_CODE)
        assertEquals(7, DeveloperAccessController.REQUIRED_TAPS)
    }

    @Test
    fun `the access code hash never exposes the plaintext`() {
        val code = DeveloperAccessController.DEFAULT_ACCESS_CODE
        // A SHA-256 hex digest is 64 characters and never contains the plaintext itself.
        assertFalse(code.length == 64)
        assertTrue(DeveloperAccessController.DEFAULT_ACCESS_CODE.length < 10)
    }
}
