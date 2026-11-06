package com.example.media

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Converts noisy accelerometer samples into distinct, intentional shake pulses.
 * A sequence is accepted only when five pulses alternate direction on the same
 * dominant axis and are spaced far enough apart to reject ordinary vibration.
 */
internal class ShakeSequenceTracker(
    private val thresholdMetersPerSecondSquared: Float = 3.7f,
    private val requiredPulses: Int = 5,
    private val minimumPulseGapMs: Long = 220L,
    private val maximumPulseGapMs: Long = 900L,
    private val releaseThresholdMetersPerSecondSquared: Float = 1.65f,
    private val releaseDurationMs: Long = 100L
) {
    private var armed = true
    private var belowThresholdSinceMs = 0L
    private var previousPulseAtMs = 0L
    private var previousAxis = -1
    private var previousSign = 0
    private var pulseCount = 0

    /** Returns true once per completed deliberate shake sequence. */
    fun addSample(x: Float, y: Float, z: Float, nowMs: Long): Boolean {
        val magnitude = sqrt(x * x + y * y + z * z)

        if (!armed) {
            if (magnitude <= releaseThresholdMetersPerSecondSquared) {
                if (belowThresholdSinceMs == 0L) belowThresholdSinceMs = nowMs
                if (nowMs - belowThresholdSinceMs >= releaseDurationMs) {
                    armed = true
                    belowThresholdSinceMs = 0L
                }
            } else {
                belowThresholdSinceMs = 0L
            }
        }

        if (!armed || magnitude < thresholdMetersPerSecondSquared) return false

        // Consume this pulse once. Further sensor samples from the same movement
        // cannot increase the count until the device has settled briefly.
        armed = false
        belowThresholdSinceMs = 0L

        val absX = abs(x)
        val absY = abs(y)
        val absZ = abs(z)
        val axis = when {
            absX >= absY && absX >= absZ -> 0
            absY >= absX && absY >= absZ -> 1
            else -> 2
        }
        val component = when (axis) {
            0 -> x
            1 -> y
            else -> z
        }
        val sign = if (component >= 0f) 1 else -1

        val gap = if (previousPulseAtMs == 0L) Long.MAX_VALUE else nowMs - previousPulseAtMs
        val isValidContinuation = gap in minimumPulseGapMs..maximumPulseGapMs &&
            axis == previousAxis && sign != previousSign

        pulseCount = if (isValidContinuation) pulseCount + 1 else 1
        previousPulseAtMs = nowMs
        previousAxis = axis
        previousSign = sign

        if (pulseCount >= requiredPulses) {
            reset()
            return true
        }
        return false
    }

    fun reset() {
        armed = true
        belowThresholdSinceMs = 0L
        previousPulseAtMs = 0L
        previousAxis = -1
        previousSign = 0
        pulseCount = 0
    }
}
