package com.ammar.wallflow.baselineprofiles

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.ammar.wallflow.test.benchmarks.PACKAGE_NAME
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(PACKAGE_NAME) {
        pressHome()
        startActivityAndWait()
    }
}
