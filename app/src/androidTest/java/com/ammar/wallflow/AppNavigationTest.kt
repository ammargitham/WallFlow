package com.ammar.wallflow

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.ammar.wallflow.activities.main.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class AppNavigationTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verifyStartDestination() {
        composeTestRule
            .onNodeWithTag("Home Screen")
            .assertIsDisplayed()
    }

    @Test
    fun navigateToWallpaperScreen() {
        composeTestRule.apply {
            waitUntilAtLeastOneExists(
                matcher = hasTestTag("wallpaper"),
                timeoutMillis = 10 * 1000,
            )
            onAllNodesWithTag("wallpaper")[0].performClick()
            waitUntilAtLeastOneExists(hasTestTag("Wallpaper Screen"))
            onNodeWithTag("Wallpaper Screen").assertIsDisplayed()
        }
    }
}
