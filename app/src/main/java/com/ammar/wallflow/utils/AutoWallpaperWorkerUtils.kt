package com.ammar.wallflow.utils

import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.SourceChoice

fun getNextSourceChoice(
    target: WallpaperTarget,
    sourceChoices: Set<SourceChoice>,
    lsSourceChoices: Set<SourceChoice>,
    prevHomeSourceChoice: SourceChoice?,
    prevLsSourceChoice: SourceChoice?,
) = when (target) {
    WallpaperTarget.HOME -> getNextSourceChoice(
        sourceChoices,
        prevHomeSourceChoice,
    )
    WallpaperTarget.LOCKSCREEN -> getNextSourceChoice(
        lsSourceChoices,
        prevLsSourceChoice,
    )
}

fun getNextSourceChoice(
    sourceChoices: Set<SourceChoice>,
    prevSource: SourceChoice?,
): SourceChoice? {
    if (prevSource == null || sourceChoices.size == 1) {
        return sourceChoices.firstOrNull()
    }
    val list = sourceChoices.toList()
    val i = list.indexOf(prevSource)
    val next = if (i == -1 || i == sourceChoices.size - 1) {
        0
    } else {
        i + 1
    }
    return list[next]
}
