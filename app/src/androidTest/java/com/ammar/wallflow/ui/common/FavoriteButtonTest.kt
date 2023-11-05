package com.ammar.wallflow.ui.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import com.ammar.wallflow.ui.screens.wallpaper.FavoriteButton
import org.junit.Rule
import org.junit.Test

class FavoriteButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testFavoriteButtonInitialState() {
        // Test the FavoriteButton in its initial state (not marked as a favorite).
        val initialFavoriteState = false

        composeTestRule.setContent {
            FavoriteButton(
                modifier = Modifier.fillMaxSize(),
                isFavorite = initialFavoriteState,
                onToggle = {},
            )
        }

        // Ensure the Favorite button is displayed.
        composeTestRule
            .onNodeWithContentDescription("Favorite")
            .assertIsDisplayed()

        // Ensure the Favorite button has a click action.
        composeTestRule
            .onNodeWithContentDescription("Favorite")
            .assertHasClickAction()

        // Ensure the initial state is not marked as a favorite.
        val favoriteIconResId = if (initialFavoriteState) {
            "baseline_favorite_24"
        } else {
            "outline_favorite_border_24"
        }
        composeTestRule
            .onNodeWithTag(favoriteIconResId)
            .assertIsDisplayed()
    }

    @Test
    fun testFavoriteButtonToggle() {
        // Test toggling the FavoriteButton.
        val isFavorite = mutableStateOf(false)

        composeTestRule.setContent {
            FavoriteButton(
                modifier = Modifier.fillMaxSize(),
                isFavorite = isFavorite.value,
                onToggle = { newValue -> isFavorite.value = newValue },
            )
        }

        // Ensure the Favorite button is displayed.
        composeTestRule
            .onNodeWithContentDescription("Favorite")
            .assertIsDisplayed()

        // Ensure the Favorite button has a click action.
        composeTestRule
            .onNodeWithContentDescription("Favorite")
            .assertHasClickAction()

        // Click the Favorite button to toggle its state.
        composeTestRule
            .onNodeWithContentDescription("Favorite")
            .performClick()

        // Ensure the state has changed.
        assert(isFavorite.value)

        composeTestRule.onRoot().printToLog(FavoriteButtonTest::class.java.simpleName)

        // Ensure the new state is marked as a favorite.
        composeTestRule
            .onNodeWithTag("baseline_favorite_24")
            .assertIsDisplayed()

        // Click the Favorite button again to toggle its state back.
        composeTestRule
            .onNodeWithContentDescription("Favorite")
            .performClick()

        // Ensure the state has changed back.
        assert(!isFavorite.value)

        // Ensure the new state is not marked as a favorite.
        composeTestRule
            .onNodeWithTag("outline_favorite_border_24")
            .assertIsDisplayed()
    }
}

