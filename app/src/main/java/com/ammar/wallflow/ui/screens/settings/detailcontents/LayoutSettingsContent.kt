package com.ammar.wallflow.ui.screens.settings.detailcontents

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.data.preferences.GridColType
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.ui.screens.settings.detailcontents.composables.LayoutPreferenceProvider
import com.ammar.wallflow.ui.screens.settings.detailcontents.composables.LayoutPreview
import com.ammar.wallflow.ui.screens.settings.detailcontents.composables.adaptiveColMinWidthPctSection
import com.ammar.wallflow.ui.screens.settings.detailcontents.composables.gridColTypeSection
import com.ammar.wallflow.ui.screens.settings.detailcontents.composables.gridTypeSection
import com.ammar.wallflow.ui.screens.settings.detailcontents.composables.noOfColumnsSection
import com.ammar.wallflow.ui.screens.settings.detailcontents.composables.roundedCornersSection
import com.ammar.wallflow.ui.theme.WallFlowTheme

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
                isExpanded = supportsTwoPane,
                onGridTypeChange = {
                    onLayoutPreferencesChange(
                        layoutPreferences.copy(gridType = it),
                    )
                },
            )
            gridColTypeSection(
                gridColType = layoutPreferences.gridColType,
                isExpanded = supportsTwoPane,
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
                    isExpanded = supportsTwoPane,
                    onMinWidthPctChange = {
                        onLayoutPreferencesChange(
                            layoutPreferences.copy(gridColMinWidthPct = it),
                        )
                    },
                )
                GridColType.FIXED -> noOfColumnsSection(
                    noOfColumns = layoutPreferences.gridColCount,
                    sliderPadding = sliderPadding,
                    isExpanded = supportsTwoPane,
                    onNoOfColumnsChange = {
                        onLayoutPreferencesChange(
                            layoutPreferences.copy(gridColCount = it),
                        )
                    },
                )
            }
            roundedCornersSection(
                roundedCorners = layoutPreferences.roundedCorners,
                isExpanded = supportsTwoPane,
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
