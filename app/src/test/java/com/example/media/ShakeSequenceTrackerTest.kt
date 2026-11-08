package com.example.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeSequenceTrackerTest {
    private fun pulse(tracker: ShakeSequenceTracker, atMs: Long, sign: Float): Boolean =
        tracker.addSample(sign * 4.5f, 0f, 0f, atMs)

    private fun settle(tracker: ShakeSequenceTracker, atMs: Long) {
        tracker.addSample(0f, 0f, 0f, atMs)
        tracker.addSample(0f, 0f, 0f, atMs + 110L)
    }

    @Test
    fun oneStrongMovementDoesNotOpenDeveloperPanel() {
        val tracker = ShakeSequenceTracker()
        assertFalse(pulse(tracker, 1_000L, 1f))
        // Multiple loud sensor readings during the same movement remain one pulse.
        assertFalse(tracker.addSample(5.2f, 0f, 0f, 1_020L))
        assertFalse(tracker.addSample(4.8f, 0f, 0f, 1_040L))
    }

    @Test
    fun fiveAlternatingDistinctPulsesTriggerExactlyOnce() {
        val tracker = ShakeSequenceTracker()
        var triggered = false
        var at = 1_000L
        repeat(5) { index ->
            val nowTriggered = pulse(tracker, at, if (index % 2 == 0) 1f else -1f)
            triggered = triggered || nowTriggered
            if (index < 4) settle(tracker, at + 100L)
            at += 350L
        }
        assertTrue(triggered)
        assertFalse(pulse(tracker, at + 10L, 1f))
    }

    @Test
    fun repeatedSameDirectionDoesNotCountAsShakeSequence() {
        val tracker = ShakeSequenceTracker()
        var at = 1_000L
        repeat(7) {
            assertFalse(pulse(tracker, at, 1f))
            settle(tracker, at + 100L)
            at += 350L
        }
    }

    @Test
    fun fastVibrationPulsesAreIgnored() {
        val tracker = ShakeSequenceTracker()
        assertFalse(pulse(tracker, 1_000L, 1f))
        // The sensor never stays settled for the required release interval.
        tracker.addSample(0f, 0f, 0f, 1_050L)
        tracker.addSample(0f, 0f, 0f, 1_100L)
        assertFalse(pulse(tracker, 1_150L, -1f))
        tracker.addSample(0f, 0f, 0f, 1_180L)
        tracker.addSample(0f, 0f, 0f, 1_220L)
        assertFalse(pulse(tracker, 1_250L, 1f))
    }
}
