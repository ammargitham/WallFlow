package com.ammar.wallflow.ui.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.ammar.wallflow.ui.screens.wallpaper.DownloadButton
import com.ammar.wallflow.utils.DownloadStatus
import org.junit.Rule
import org.junit.Test

class DownloadButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDownloadButtonInitialState() {
        // Test the DownloadButton in its initial state.
        composeTestRule.setContent {
            DownloadButton(
                modifier = Modifier.fillMaxSize(),
                downloadStatus = null,
                onClick = {},
            )
        }

        // Ensure the button is displayed.
        composeTestRule
            .onNodeWithContentDescription("Download")
            .assertIsDisplayed()

        // Ensure the button has a click action.
        composeTestRule
            .onNodeWithContentDescription("Download")
            .assertHasClickAction()
    }

    @Test
    fun testDownloadButtonRunningState() {
        // Test the DownloadButton in a "Running" state.
        val downloadedBytes = 50L
        val totalBytes = 100L

        composeTestRule.setContent {
            DownloadButton(
                modifier = Modifier.fillMaxSize(),
                downloadStatus = DownloadStatus.Running(downloadedBytes, totalBytes),
                onClick = {},
            )
        }

        // Ensure the button is displayed.
        composeTestRule
            .onNodeWithContentDescription("Download")
            .assertIsDisplayed()

        // Ensure the button has a click action.
        composeTestRule
            .onNodeWithContentDescription("Download")
            .assertHasClickAction()

        // Ensure the progress indicator is displayed.
        composeTestRule
            .onNodeWithTag("circular-progress")
            .assertIsDisplayed()

        // Ensure the progress text is displayed.
        val progress = downloadedBytes.toFloat() / totalBytes
        composeTestRule
            .onNodeWithContentDescription("Progress $progress")
            .assertIsDisplayed()
    }

    @Test
    fun testDownloadButtonPausedState() {
        // Test the DownloadButton in a "Paused" state.
        val downloadedBytes = 50L
        val totalBytes = 100L

        composeTestRule.setContent {
            DownloadButton(
                modifier = Modifier.fillMaxSize(),
                downloadStatus = DownloadStatus.Paused(downloadedBytes, totalBytes),
                onClick = {},
            )
        }

        // Ensure the button is displayed.
        composeTestRule
            .onNodeWithContentDescription("Download")
            .assertIsDisplayed()

        // Ensure the button has a click action.
        composeTestRule
            .onNodeWithContentDescription("Download")
            .assertHasClickAction()

        // Ensure the pause icon is displayed.
        composeTestRule
            .onNodeWithContentDescription("Paused")
            .assertIsDisplayed()

        // Ensure the progress indicator is displayed.
        composeTestRule
            .onNodeWithTag("circular-progress")
            .assertIsDisplayed()

        // Ensure the progress text is displayed.
        val progress = downloadedBytes.toFloat() / totalBytes
        composeTestRule
            .onNodeWithContentDescription("Progress $progress")
            .assertIsDisplayed()
    }

    @Test
    fun testDownloadButtonSuccessState() {
        // Test the DownloadButton in a "Success" state.
        val filePath = "sample/path/to/file.txt"

        composeTestRule.setContent {
            DownloadButton(
                modifier = Modifier.fillMaxSize(),
                downloadStatus = DownloadStatus.Success(filePath),
                onClick = {},
            )
        }

        // Ensure the button is displayed.
        composeTestRule
            .onNodeWithContentDescription("Download")
            .assertIsDisplayed()

        // Ensure the button has a click action.
        composeTestRule
            .onNodeWithContentDescription("Download")
            .assertHasClickAction()

        // Ensure the success icon is displayed.
        composeTestRule
            .onNodeWithContentDescription("Success")
            .assertIsDisplayed()
    }

    @Test
    fun testDownloadButtonFailedState() {
        // Test the DownloadButton in a "Failed" state.
        composeTestRule.setContent {
            DownloadButton(
                modifier = Modifier.fillMaxSize(),
                downloadStatus = DownloadStatus.Failed(Exception()),
                onClick = {},
            )
        }

        // Ensure the button is displayed.
        composeTestRule
            .onNodeWithContentDescription("Download")
            .assertIsDisplayed()

        // Ensure the button has a click action.
        composeTestRule
            .onNodeWithContentDescription("Download")
            .assertHasClickAction()

        // Ensure the error icon is displayed.
        composeTestRule
            .onNodeWithContentDescription("Failed")
            .assertIsDisplayed()
    }
}

