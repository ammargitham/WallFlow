package com.ammar.wallflow

import kotlin.test.assertTrue
import org.junit.Test

class ConstantsTest {
    @Test
    fun `test subreddit regex`() {
        val validNames = listOf(
            "aaa",
            "aaaaaaaaaaaaaaaaaaaaa",
            "r/aaaaaaaaaaaaaaaaaaaaa",
            "/r/aaaaaaaaaaaaaaaaaaaaa",
            "MobileWallpapers",
            "r/MobileWallpapers",
            "/r/MobileWallpapers",
            "mobile_wallpapers",
            "Mobile_Wallpapers",
            "r/mobile_wallpapers",
            "/r/mobile_wallpapers",
            "r/Mobile_Wallpapers",
            "/r/Mobile_Wallpapers",
        )
        val invalidNames = listOf(
            "",
            "aa",
            "r/aa",
            "/r/aa",
            "aaaaaaaaaaaaaaaaaaaaaa",
            "r/aaaaaaaaaaaaaaaaaaaaaa",
            "/r/aaaaaaaaaaaaaaaaaaaaaa",
            "_aa",
            "r/_aa",
            "/r/_aa",
        )
        validNames.forEach {
            assertTrue(SUBREDDIT_REGEX.matches(it), "\"$it\" should match regex")
        }
        invalidNames.forEach {
            assertTrue(!SUBREDDIT_REGEX.matches(it), "\"$it\" should not match regex")
        }
    }
}
