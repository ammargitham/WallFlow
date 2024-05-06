package com.ammar.wallflow.utils

import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.SourceChoice

fun getNextSourceChoice(
    target: WallpaperTarget,
    sourceChoices: Set<SourceChoice>,
    lsSourceChoices: Set<SourceChoice>,
    prevHomeSourceChoice: SourceChoice?,
    prevLsSourceChoice: SourceChoice?,
    excluding: List<SourceChoice> = emptyList(),
) = when (target) {
    WallpaperTarget.HOME -> getNextSourceChoice(
        sourceChoices = sourceChoices,
        prevSourceChoice = prevHomeSourceChoice,
        excluding = excluding,
    )
    WallpaperTarget.LOCKSCREEN -> getNextSourceChoice(
        sourceChoices = lsSourceChoices,
        prevSourceChoice = prevLsSourceChoice,
        excluding = excluding,
    )
}

fun getNextSourceChoice(
    sourceChoices: Set<SourceChoice>,
    prevSourceChoice: SourceChoice?,
    excluding: List<SourceChoice> = emptyList(),
): SourceChoice? {
    if (prevSourceChoice == null || sourceChoices.size == 1) {
        return sourceChoices.firstOrNull { it !in excluding }
    }
    val list = sourceChoices.toList()
    var next: SourceChoice
    var startIndex = list.indexOf(prevSourceChoice)
    var loopCount = 1
    // loop until we exhaust all choices
    while (loopCount <= sourceChoices.size) {
        // get the next index after prevSource
        val nextIndex = getNextIndex(
            index = startIndex,
            sourceChoices = sourceChoices,
        )
        // get the choice at this index
        next = list[nextIndex]
        // check if this sourceChoice is excluded
        if (next !in excluding) {
            // if not, return it
            return next
        }
        // re-start loop at next index
        startIndex = nextIndex
        loopCount += 1
    }
    return null
}

private fun getNextIndex(
    index: Int,
    sourceChoices: Set<SourceChoice>,
) = if (index == -1 || index == sourceChoices.size - 1) {
    0
} else {
    index + 1
}
