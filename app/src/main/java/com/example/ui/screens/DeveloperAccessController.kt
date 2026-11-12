package com.example.ui.screens

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.util.Locale

/**
 * The gate in front of the developer panel.
 *
 * The panel must never be reachable by accident, so access requires *two* independent factors:
 *
 *  1. A deliberate gesture — seven taps on the version number, a long press on the logo, or the
 *     physical shake. The gesture only *reveals* the access dialog; it never opens the panel.
 *  2. A valid access code. The code is stored as a SHA-256 hash, so the plaintext never leaves
 *     the user's memory.
 *
 * The unlocked state is stored in a dedicated preferences file with a short expiry, and it is
 * cleared explicitly by the "leave developer mode" button.
 */
object DeveloperAccessController {

    private const val PREFS_NAME = "alhadithi_developer_access"
    private const val KEY_CODE_HASH = "access_code_hash"
    private const val KEY_UNLOCKED_UNTIL = "unlocked_until_millis"

    /** How long an unlocked session stays valid. */
    private const val SESSION_DURATION_MILLIS = 30L * 60L * 1000L

    /** The default access code. It is intentionally short and documented for the owner. */
    const val DEFAULT_ACCESS_CODE = "7419"

    /** The number of taps on the version number that reveals the access dialog. */
    const val REQUIRED_TAPS = 7

    /** The maximum interval between two consecutive taps. */
    private const val TAP_WINDOW_MILLIS = 1_500L

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns true when the user tapped the version number [REQUIRED_TAPS] times within the
     * allowed window. The counter resets when the taps are too far apart.
     */
    fun isMultiTapGesture(currentCount: Int, lastTapMillis: Long, nowMillis: Long): Boolean {
        if (nowMillis - lastTapMillis > TAP_WINDOW_MILLIS) return false
        return currentCount + 1 >= REQUIRED_TAPS
    }

    /** Stores the hash of the access code the first time it is set. */
    fun setAccessCode(context: Context, code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return
        prefs(context).edit().putString(KEY_CODE_HASH, hash(trimmed)).apply()
    }

    /** Verifies the entered access code and opens a temporary session on success. */
    fun verifyAndUnlock(context: Context, enteredCode: String): Boolean {
        val stored = prefs(context).getString(KEY_CODE_HASH, null)
        val expectedHash = stored ?: hash(DEFAULT_ACCESS_CODE)
        if (hash(enteredCode.trim()) != expectedHash) return false
        prefs(context).edit()
            .putLong(KEY_UNLOCKED_UNTIL, System.currentTimeMillis() + SESSION_DURATION_MILLIS)
            .apply()
        return true
    }

    /** True while the temporary developer session is still valid. */
    fun isUnlocked(context: Context): Boolean {
        val until = prefs(context).getLong(KEY_UNLOCKED_UNTIL, 0L)
        return until > System.currentTimeMillis()
    }

    /** Ends the developer session immediately. */
    fun lock(context: Context) {
        prefs(context).edit().remove(KEY_UNLOCKED_UNTIL).apply()
    }

    /** A short, non-reversible fingerprint of the device used by the diagnostics panel. */
    fun deviceFingerprint(context: Context): String {
        val raw = "${context.packageName}:${android.os.Build.MANUFACTURER}:${android.os.Build.MODEL}"
        return hash(raw).take(12).uppercase(Locale.ROOT)
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
