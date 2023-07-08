package com.ammar.wallflow.model

import android.app.WallpaperManager
import org.junit.Test
import kotlin.test.assertEquals

class WallpaperTargetTest {
    @Test
    fun convert_target_set_to_which_int() {
        var targetSet = emptySet<WallpaperTarget>()
        var expected = 0
        assertEquals(expected, targetSet.toWhichInt())

        targetSet = setOf(WallpaperTarget.HOME)
        expected = WallpaperManager.FLAG_SYSTEM
        assertEquals(expected, targetSet.toWhichInt())

        targetSet = setOf(WallpaperTarget.LOCKSCREEN)
        expected = WallpaperManager.FLAG_LOCK
        assertEquals(expected, targetSet.toWhichInt())

        targetSet = setOf(WallpaperTarget.HOME, WallpaperTarget.LOCKSCREEN)
        expected = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        assertEquals(expected, targetSet.toWhichInt())
    }
}
