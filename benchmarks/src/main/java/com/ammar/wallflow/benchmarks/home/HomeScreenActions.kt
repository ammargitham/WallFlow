package com.ammar.wallflow.benchmarks.home

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import com.ammar.wallflow.flingElementDownUp
import com.ammar.wallflow.untilHasChildrenWithRes
import com.ammar.wallflow.waitAndFindObject

fun MacrobenchmarkScope.homeWaitForContent() {
    val obj = device.waitAndFindObject(By.res("home:feed"), 10_000)
    // Timeout here is quite big, because data loading could fail or take a long time!
    obj.wait(untilHasChildrenWithRes("wallpaper"), 60_000)
}

fun MacrobenchmarkScope.homeScrollFeedDownUp() {
    val feed = device.findObject(By.res("home:feed"))
    device.flingElementDownUp(feed)
}
