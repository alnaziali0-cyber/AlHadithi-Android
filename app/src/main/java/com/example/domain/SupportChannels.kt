package com.example.domain

import java.net.URI

/** Public support destinations shown in Settings. Only HTTPS profile URLs are accepted. */
object SupportChannels {
    const val SUPPORT_EMAIL = "alhadithiiraq@gmail.com"
    const val INSTAGRAM_HANDLE = "@alhadithi_iraq"
    const val INSTAGRAM_URL = "https://www.instagram.com/alhadithi_iraq/"
    const val TIKTOK_HANDLE = "@.al.hadithi"
    const val TIKTOK_URL = "https://www.tiktok.com/@.al.hadithi"

    /**
     * Strict allow-list validation prevents accidental opening of a look-alike host,
     * non-HTTPS URL, embedded credentials, or a different profile path.
     */
    fun isSafeHttpsProfile(url: String, expectedHost: String, expectedPath: String): Boolean {
        val parsed = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        return parsed.scheme.equals("https", ignoreCase = true) &&
            parsed.host.equals(expectedHost, ignoreCase = true) &&
            parsed.rawUserInfo == null &&
            (parsed.port == -1 || parsed.port == 443) &&
            parsed.rawPath == expectedPath &&
            parsed.rawQuery == null &&
            parsed.rawFragment == null
    }
}
