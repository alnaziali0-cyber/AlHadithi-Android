package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportChannelsTest {
    @Test
    fun configuredContactDetailsMatchTheProvidedSupportChannels() {
        assertEquals("alhadithiiraq@gmail.com", SupportChannels.SUPPORT_EMAIL)
        assertEquals("@alhadithi_iraq", SupportChannels.INSTAGRAM_HANDLE)
        assertEquals("@.al.hadithi", SupportChannels.TIKTOK_HANDLE)
    }

    @Test
    fun officialInstagramProfileIsAccepted() {
        assertTrue(
            SupportChannels.isSafeHttpsProfile(
                SupportChannels.INSTAGRAM_URL,
                "www.instagram.com",
                "/alhadithi_iraq/"
            )
        )
    }

    @Test
    fun officialTikTokProfileIsAccepted() {
        assertTrue(
            SupportChannels.isSafeHttpsProfile(
                SupportChannels.TIKTOK_URL,
                "www.tiktok.com",
                "/@.al.hadithi"
            )
        )
    }

    @Test
    fun rejectsHttpLookalikeAndUnexpectedProfileUrls() {
        assertFalse(
            SupportChannels.isSafeHttpsProfile(
                "http://www.instagram.com/alhadithi_iraq/",
                "www.instagram.com",
                "/alhadithi_iraq/"
            )
        )
        assertFalse(
            SupportChannels.isSafeHttpsProfile(
                "https://www.instagram.com.evil.example/alhadithi_iraq/",
                "www.instagram.com",
                "/alhadithi_iraq/"
            )
        )
        assertFalse(
            SupportChannels.isSafeHttpsProfile(
                "https://attacker@www.instagram.com/alhadithi_iraq/",
                "www.instagram.com",
                "/alhadithi_iraq/"
            )
        )
        assertFalse(
            SupportChannels.isSafeHttpsProfile(
                "https://www.tiktok.com/@someone_else",
                "www.tiktok.com",
                "/@.al.hadithi"
            )
        )
        assertFalse(
            SupportChannels.isSafeHttpsProfile(
                "https://www.instagram.com/alhadithi_iraq/?redirect=https://evil.example",
                "www.instagram.com",
                "/alhadithi_iraq/"
            )
        )
    }
}
