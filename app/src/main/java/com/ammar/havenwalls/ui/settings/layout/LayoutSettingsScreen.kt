package com.ammar.havenwalls.ui.settings.layout

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ammar.havenwalls.R
import com.ammar.havenwalls.data.preferences.GridType
import com.ammar.havenwalls.data.preferences.LayoutPreferences
import com.ammar.havenwalls.data.preferences.maxGridCols
import com.ammar.havenwalls.data.preferences.minGridCols
import com.ammar.havenwalls.ui.common.TopBar
import com.ammar.havenwalls.ui.common.bottomWindowInsets
import com.ammar.havenwalls.ui.common.bottombar.LocalBottomBarController
import com.ammar.havenwalls.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.havenwalls.ui.common.mainsearch.MainSearchBarState
import com.ammar.havenwalls.ui.common.navigation.TwoPaneNavigation
import com.ammar.havenwalls.ui.common.navigation.TwoPaneNavigation.Mode
import com.ammar.havenwalls.ui.theme.HavenWallsTheme
import com.ramcosta.composedestinations.annotation.Destination
import kotlin.math.roundToInt

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
            .windowInsetsPadding(bottomWindowInsets)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutSettingsScreenContent(
    modifier: Modifier = Modifier,
    supportsTwoPane: Boolean = false,
    layoutPreferences: LayoutPreferences = LayoutPreferences(),
    onLayoutPreferencesChange: (LayoutPreferences) -> Unit = {},
) {
    val context = LocalContext.current
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
            item {
                ListItem(
                    headlineContent = {
                        Text(text = stringResource(R.string.grid_type))
                    },
                    trailingContent = {
                        val options = GridType.values().associateWith { getLabelForGridType(it) }
                        var expanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(0.5f),
                                readOnly = true,
                                value = options[layoutPreferences.gridType] ?: "",
                                onValueChange = {},
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                options.forEach { (type, label) ->
                                    DropdownMenuItem(
                                        text = { Text(text = label) },
                                        onClick = {
                                            onLayoutPreferencesChange(
                                                layoutPreferences.copy(gridType = type)
                                            )
                                            expanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                    )
                                }
                            }
                        }
                    }
                )
            }
            item {
                val sliderPosition = layoutPreferences.gridColCount.toFloat()
                ListItem(
                    headlineContent = {
                        Text(text = stringResource(R.string.no_of_columns))
                    },
                    supportingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                modifier = Modifier.widthIn(min = sliderPadding),
                                text = sliderPosition.roundToInt().toString(),
                                textAlign = TextAlign.Center,
                            )
                            Slider(
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics {
                                        contentDescription = context.getString(
                                            R.string.no_of_columns
                                        )
                                    },
                                value = sliderPosition,
                                onValueChange = {
                                    onLayoutPreferencesChange(
                                        layoutPreferences.copy(gridColCount = it.roundToInt())
                                    )
                                },
                                valueRange = minGridCols.toFloat()..maxGridCols.toFloat(),
                                onValueChangeFinished = {
                                    // launch some business logic update with the state you hold
                                    // viewModel.updateSelectedSliderValue(sliderPosition)
                                },
                                steps = (maxGridCols - minGridCols + 1).toInt(),
                            )
                            Spacer(modifier = Modifier.width(sliderPadding))
                        }
                    }
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        onLayoutPreferencesChange(
                            layoutPreferences.copy(roundedCorners = !layoutPreferences.roundedCorners)
                        )
                    },
                    headlineContent = { Text(text = stringResource(R.string.rounded_corners)) },
                    trailingContent = {
                        Switch(
                            modifier = Modifier.height(24.dp),
                            checked = layoutPreferences.roundedCorners,
                            onCheckedChange = {
                                onLayoutPreferencesChange(
                                    layoutPreferences.copy(roundedCorners = it)
                                )
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun getLabelForGridType(gridType: GridType) = when (gridType) {
    GridType.STAGGERED -> stringResource(R.string.staggered)
    GridType.FIXED_SIZE -> stringResource(R.string.fixed_size)
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewLayoutSettingsScreenContent(
    @PreviewParameter(LayoutPreferenceProvider::class) twoPaneLayoutPreferences: Pair<Boolean, LayoutPreferences>,
) {
    var tempLayoutPreferences by remember { mutableStateOf(twoPaneLayoutPreferences.second) }

    HavenWallsTheme {
        Surface {
            LayoutSettingsScreenContent(
                supportsTwoPane = twoPaneLayoutPreferences.first,
                layoutPreferences = tempLayoutPreferences,
                onLayoutPreferencesChange = { tempLayoutPreferences = it }
            )
        }
    }
}
