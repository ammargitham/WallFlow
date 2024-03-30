package com.ammar.wallflow.utils

import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.SourceChoice
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import org.junit.Test

class AutoWallpaperWorkerUtilsTest {
    @Test
    fun `test choose different source choice every call`() {
        var prevHomeChoice: SourceChoice? = null
        var target = WallpaperTarget.HOME
        var choices = SourceChoice.entries.toSet()
        repeat(10) {
            val nextChoice = getNextSourceChoice(
                target = target,
                sourceChoices = choices,
                lsSourceChoices = emptySet(),
                prevHomeSourceChoice = prevHomeChoice,
                prevLsSourceChoice = null,
            )
            assertNotNull(nextChoice)
            assertNotEquals(nextChoice, prevHomeChoice)
            prevHomeChoice = nextChoice
        }

        var prevLsChoice: SourceChoice? = null
        target = WallpaperTarget.LOCKSCREEN
        choices = SourceChoice.entries.toSet()
        repeat(10) {
            val nextChoice = getNextSourceChoice(
                target = target,
                sourceChoices = emptySet(),
                lsSourceChoices = choices,
                prevHomeSourceChoice = null,
                prevLsSourceChoice = prevLsChoice,
            )
            assertNotNull(nextChoice)
            assertNotEquals(nextChoice, prevLsChoice)
            prevLsChoice = nextChoice
        }
    }
}
