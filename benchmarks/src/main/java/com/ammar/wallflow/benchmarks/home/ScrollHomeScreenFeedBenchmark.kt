package com.ammar.wallflow.benchmarks.home

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.ammar.wallflow.test.benchmarks.PACKAGE_NAME
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4ClassRunner::class)
@LargeTest
class ScrollHomeScreenFeedBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun scrollFeedCompilationNone() = scrollFeed(CompilationMode.None())

    @Test
    fun scrollFeedCompilationBaselineProfile() = scrollFeed(CompilationMode.Partial())

    private fun scrollFeed(compilationMode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = compilationMode,
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
        },
    ) {
        homeWaitForContent()
        homeScrollFeedDownUp()
    }
}
