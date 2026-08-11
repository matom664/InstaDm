package com.example.instagramwrapper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InstagramUrlFilterTest {
    @Test
    fun `home feed is allowed`() {
        assertTrue(InstagramUrlFilter.isInstagramUrl("https://www.instagram.com/"))
        assertFalse(InstagramUrlFilter.isBlockedInstagramUrl("https://www.instagram.com/"))
    }

    @Test
    fun `direct urls are allowed`() {
        assertTrue(InstagramUrlFilter.isInstagramUrl("https://www.instagram.com/direct/"))
        assertTrue(InstagramUrlFilter.isInstagramUrl("https://www.instagram.com/direct/inbox/"))
        assertFalse(InstagramUrlFilter.isBlockedInstagramUrl("https://www.instagram.com/direct/"))
        assertFalse(InstagramUrlFilter.isBlockedInstagramUrl("https://www.instagram.com/direct/inbox/"))
    }

    @Test
    fun `explore posts and profiles are allowed`() {
        assertTrue(InstagramUrlFilter.isInstagramUrl("https://www.instagram.com/explore/"))
        assertTrue(InstagramUrlFilter.isInstagramUrl("https://www.instagram.com/p/ABC123/"))
        assertTrue(InstagramUrlFilter.isInstagramUrl("https://www.instagram.com/username/"))
        assertFalse(InstagramUrlFilter.isBlockedInstagramUrl("https://www.instagram.com/explore/"))
        assertFalse(InstagramUrlFilter.isBlockedInstagramUrl("https://www.instagram.com/p/ABC123/"))
        assertFalse(InstagramUrlFilter.isBlockedInstagramUrl("https://www.instagram.com/username/"))
    }

    @Test
    fun `reels are blocked with and without trailing slashes or query parameters`() {
        assertTrue(InstagramUrlFilter.isInstagramUrl("https://www.instagram.com/reels"))
        assertTrue(InstagramUrlFilter.isInstagramUrl("https://www.instagram.com/reels/"))
        assertTrue(InstagramUrlFilter.isInstagramUrl("https://www.instagram.com/reels/ABC123/"))
        assertTrue(InstagramUrlFilter.isInstagramUrl("https://www.instagram.com/reels/?foo=bar"))
        assertTrue(InstagramUrlFilter.isInstagramUrl("https://www.instagram.com/reels/ABC?foo=bar#test"))
        assertTrue(InstagramUrlFilter.isBlockedInstagramUrl("https://www.instagram.com/reels"))
        assertTrue(InstagramUrlFilter.isBlockedInstagramUrl("https://www.instagram.com/reels/"))
        assertTrue(InstagramUrlFilter.isBlockedInstagramUrl("https://www.instagram.com/reels/ABC123/"))
        assertTrue(InstagramUrlFilter.isBlockedInstagramUrl("https://www.instagram.com/reels/?foo=bar"))
        assertTrue(InstagramUrlFilter.isBlockedInstagramUrl("https://www.instagram.com/reels/ABC?foo=bar#test"))
    }

    @Test
    fun `host case and scheme case are handled`() {
        assertTrue(InstagramUrlFilter.isInstagramUrl("HTTPS://WWW.INSTAGRAM.COM/direct/"))
        assertFalse(InstagramUrlFilter.isBlockedInstagramUrl("HTTPS://WWW.INSTAGRAM.COM/direct/"))
    }

    @Test
    fun `non instagram and malformed urls are rejected`() {
        assertFalse(InstagramUrlFilter.isInstagramUrl("https://example.com/"))
        assertFalse(InstagramUrlFilter.isInstagramUrl("not a url"))
        assertFalse(InstagramUrlFilter.isInstagramUrl("javascript:alert(1)"))
        assertFalse(InstagramUrlFilter.isBlockedInstagramUrl("https://example.com/reels"))
    }

    @Test
    fun `restore url normalization upgrades instagram http links to https`() {
        assertTrue(
            InstagramUrlFilter.normalizeAllowedInstagramUrl("http://www.instagram.com/direct/") ==
                "https://www.instagram.com/direct/"
        )
    }
}
