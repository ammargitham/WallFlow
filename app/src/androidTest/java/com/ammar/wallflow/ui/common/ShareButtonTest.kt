package com.ammar.wallflow.ui.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ammar.wallflow.ui.screens.wallpaper.ShareButton
import org.junit.Rule
import org.junit.Test

class ShareButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testShareButtonInitialState() {
        // Test the ShareButton in its initial state.
        composeTestRule.setContent {
            ShareButton(
                modifier = Modifier.fillMaxSize(),
                showShareLinkAction = true,
                onLinkClick = {},
                onImageClick = {},
            )
        }

        // Ensure the Share button is displayed.
        composeTestRule
            .onNodeWithContentDescription("Share")
            .assertIsDisplayed()

        // Ensure the Share button has a click action.
        composeTestRule
            .onNodeWithContentDescription("Share")
            .assertHasClickAction()

        // Ensure the dropdown menu is not expanded.
        composeTestRule
            .onNodeWithContentDescription("Menu")
            .assertDoesNotExist()
    }

    @Test
    fun testShareButtonWithLinkAction() {
        // Test the ShareButton when the link action is shown.
        val linkClickCount = mutableListOf<Int>()
        val imageClickCount = mutableListOf<Int>()

        composeTestRule.setContent {
            ShareButton(
                modifier = Modifier.fillMaxSize(),
                showShareLinkAction = true,
                onLinkClick = { linkClickCount.add(1) },
                onImageClick = { imageClickCount.add(1) },
            )
        }

        // Ensure the Share button is displayed.
        composeTestRule
            .onNodeWithContentDescription("Share")
            .assertIsDisplayed()

        // Ensure the Share button has a click action.
        composeTestRule
            .onNodeWithContentDescription("Share")
            .assertHasClickAction()

        // Click the Share button.
        composeTestRule
            .onNodeWithContentDescription("Share")
            .performClick()

        // Ensure the dropdown menu is expanded.
        composeTestRule
            .onNodeWithContentDescription("Menu")
            .assertIsDisplayed()

        // Ensure the "Link" option is displayed.
        composeTestRule
            .onNodeWithText("Link")
            .assertIsDisplayed()

        // Click the "Link" option.
        composeTestRule
            .onNodeWithText("Link")
            .performClick()

        // Ensure the dropdown menu is closed.
        composeTestRule
            .onNodeWithContentDescription("Menu")
            .assertDoesNotExist()

        // Ensure the linkClickCount was incremented.
        assert(linkClickCount.size == 1)

        // Ensure the imageClickCount is empty.
        assert(imageClickCount.isEmpty())
    }

    @Test
    fun testShareButtonWithoutLinkAction() {
        // Test the ShareButton when the link action is not shown.
        val linkClickCount = mutableListOf<Int>()
        val imageClickCount = mutableListOf<Int>()

        composeTestRule.setContent {
            ShareButton(
                modifier = Modifier.fillMaxSize(),
                showShareLinkAction = false,
                onLinkClick = { linkClickCount.add(1) },
                onImageClick = { imageClickCount.add(1) },
            )
        }

        // Ensure the Share button is displayed.
        composeTestRule
            .onNodeWithContentDescription("Share")
            .assertIsDisplayed()

        // Ensure the Share button has a click action.
        composeTestRule
            .onNodeWithContentDescription("Share")
            .assertHasClickAction()

        // Click the Share button.
        composeTestRule
            .onNodeWithContentDescription("Share")
            .performClick()

        // Ensure the dropdown menu is not expanded.
        composeTestRule
            .onNodeWithContentDescription("Menu")
            .assertDoesNotExist()

        // Ensure the linkClickCount is empty.
        assert(linkClickCount.isEmpty())

        // Ensure the imageClickCount was incremented.
        assert(imageClickCount.size == 1)
    }
}
