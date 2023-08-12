package com.ammar.wallflow.ui.settings.layout

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.GridColType
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.ui.common.TopBar
import com.ammar.wallflow.ui.common.bottomWindowInsets
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.wallflow.ui.common.mainsearch.MainSearchBarState
import com.ammar.wallflow.ui.common.navigation.TwoPaneNavigation
import com.ammar.wallflow.ui.common.navigation.TwoPaneNavigation.Mode
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ramcosta.composedestinations.annotation.Destination

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun LayoutSettingsScreen(
    twoPaneController: TwoPaneNavigation.Controller,
    viewModel: LayoutSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchBarController = LocalMainSearchBarController.current
    val bottomBarController = LocalBottomBarController.current
    val supportsTwoPane = twoPaneController.supportsTwoPane

    LaunchedEffect(Unit) {
        twoPaneController.setPaneMode(Mode.SINGLE_PANE) // hide pane 2
        searchBarController.update { MainSearchBarState(visible = false) }
        bottomBarController.update { it.copy(visible = false) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(bottomWindowInsets),
    ) {
        TopBar(
            navController = twoPaneController.pane1NavHostController,
            title = {
                Text(
                    text = stringResource(R.string.layout),
                    maxLines = 1,
                )
            },
            showBackButton = true,
        )
        LayoutSettingsScreenContent(
            supportsTwoPane = supportsTwoPane,
            layoutPreferences = uiState.appPreferences.lookAndFeelPreferences.layoutPreferences,
            onLayoutPreferencesChange = viewModel::updatePreferences,
        )
    }
}

@Composable
fun LayoutSettingsScreenContent(
    modifier: Modifier = Modifier,
    supportsTwoPane: Boolean = false,
    layoutPreferences: LayoutPreferences = LayoutPreferences(),
    onLayoutPreferencesChange: (LayoutPreferences) -> Unit = {},
) {
    val layoutDirection = LocalLayoutDirection.current
    val paddingValues = WindowInsets.mandatorySystemGestures.asPaddingValues()
    val sliderPadding = remember(paddingValues) {
        maxOf(
            paddingValues.calculateTopPadding(),
            paddingValues.calculateBottomPadding(),
            paddingValues.calculateLeftPadding(layoutDirection),
            paddingValues.calculateRightPadding(layoutDirection),
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        LayoutPreview(
            modifier = Modifier.fillMaxWidth(),
            supportsTwoPane = supportsTwoPane,
            layoutPreferences = layoutPreferences,
        )
        LazyColumn(
            modifier = modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            gridTypeSection(
                gridType = layoutPreferences.gridType,
                onGridTypeChange = {
                    onLayoutPreferencesChange(
                        layoutPreferences.copy(gridType = it),
                    )
                },
            )
            gridColTypeSection(
                gridColType = layoutPreferences.gridColType,
                onGridColTypeChange = {
                    onLayoutPreferencesChange(
                        layoutPreferences.copy(gridColType = it),
                    )
                },
            )
            when (layoutPreferences.gridColType) {
                GridColType.ADAPTIVE -> adaptiveColMinWidthPctSection(
                    minWidthPct = layoutPreferences.gridColMinWidthPct,
                    sliderPadding = sliderPadding,
                    onMinWidthPctChange = {
                        onLayoutPreferencesChange(
                            layoutPreferences.copy(gridColMinWidthPct = it),
                        )
                    },
                )
                GridColType.FIXED -> noOfColumnsSection(
                    noOfColumns = layoutPreferences.gridColCount,
                    sliderPadding = sliderPadding,
                    onNoOfColumnsChange = {
                        onLayoutPreferencesChange(
                            layoutPreferences.copy(gridColCount = it),
                        )
                    },
                )
            }
            roundedCornersSection(
                roundedCorners = layoutPreferences.roundedCorners,
                onRoundedCornersChange = {
                    onLayoutPreferencesChange(
                        layoutPreferences.copy(roundedCorners = it),
                    )
                },
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewLayoutSettingsScreenContent(
    @PreviewParameter(LayoutPreferenceProvider::class) twoPaneLayoutPreferences:
    Pair<Boolean, LayoutPreferences>,
) {
    var tempLayoutPreferences by remember { mutableStateOf(twoPaneLayoutPreferences.second) }

    WallFlowTheme {
        Surface {
            LayoutSettingsScreenContent(
                supportsTwoPane = twoPaneLayoutPreferences.first,
                layoutPreferences = tempLayoutPreferences,
                onLayoutPreferencesChange = { tempLayoutPreferences = it },
            )
        }
    }
}
