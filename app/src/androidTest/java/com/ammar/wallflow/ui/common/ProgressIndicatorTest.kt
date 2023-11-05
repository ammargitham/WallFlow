package com.ammar.wallflow.ui.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class ProgressIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCircularProgressIndicator() {
        // Test Circular Progress Indicator
        composeTestRule.setContent {
            ProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                circular = true,
            )
        }

        composeTestRule
            .onNodeWithTag("circular-progress")
            .assertIsDisplayed()
    }

    @Test
    fun testLinearProgressIndicator() {
        // Test Linear Progress Indicator
        composeTestRule.setContent {
            ProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                circular = false,
            )
        }

        composeTestRule
            .onNodeWithTag("linear-progress")
            .assertIsDisplayed()
    }

    @Test
    fun testCircularProgressIndicatorWithProgress() {
        val progress = 0.5f

        // Test Circular Progress Indicator with progress
        composeTestRule.setContent {
            ProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                circular = true,
                progress = progress,
            )
        }

        composeTestRule
            .onNodeWithTag("circular-progress")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Progress $progress")
            .assertIsDisplayed()
    }

    @Test
    fun testLinearProgressIndicatorWithProgress() {
        val progress = 0.5f

        // Test Linear Progress Indicator with progress
        composeTestRule.setContent {
            ProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                circular = false,
                progress = progress,
            )
        }

        composeTestRule
            .onNodeWithTag("linear-progress")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Progress $progress")
            .assertIsDisplayed()
    }
}

