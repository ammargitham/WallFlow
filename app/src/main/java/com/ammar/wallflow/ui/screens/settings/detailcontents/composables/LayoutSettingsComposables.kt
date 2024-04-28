package com.ammar.wallflow.ui.screens.settings.detailcontents.composables

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.COMMON_RESOLUTIONS
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.GridColType
import com.ammar.wallflow.data.preferences.GridType
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.data.preferences.MAX_GRID_COLS
import com.ammar.wallflow.data.preferences.MAX_GRID_COL_WIDTH_PCT
import com.ammar.wallflow.data.preferences.MIN_GRID_COLS
import com.ammar.wallflow.data.preferences.MIN_GRID_COL_WIDTH_PCT
import com.ammar.wallflow.extensions.aspectRatio
import com.ammar.wallflow.extensions.getScreenResolution
import com.ammar.wallflow.extensions.toDp
import com.ammar.wallflow.extensions.toPxF
import com.ammar.wallflow.ui.screens.settings.composables.SettingsExtraListItem
import com.ammar.wallflow.ui.theme.WallFlowTheme
import kotlin.random.Random

private val random = Random(seed = 206)
private val resolutions = COMMON_RESOLUTIONS.values
    .asSequence()
    .shuffled(random)
    .take(15)
    .toList()

@Composable
internal fun LayoutPreview(
    modifier: Modifier = Modifier,
    supportsTwoPane: Boolean = false,
    layoutPreferences: LayoutPreferences = LayoutPreferences(),
) {
    val context = LocalContext.current
    var gridSize by remember { mutableStateOf(IntSize.Zero) }
    val screenResolution = remember(context) {
        context.getScreenResolution(true)
    }
    val deviceAspectRatio = screenResolution.let {
        if (it == IntSize.Zero) {
            9f / 16
        } else {
            it.aspectRatio
        }
    }
    val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val paddingDp = 4.dp
    val paddingPx = paddingDp.toPxF()
    val cornerRadiusPx by animateFloatAsState(
        targetValue = if (layoutPreferences.roundedCorners) paddingPx else 0f,
        label = "cornerRadiusPx",
    )
    val maxDeviceHeight = screenResolution.height.toDp() / 4
    val gridWidthDp = gridSize.width.toDp()
    val adaptiveMinWidth = remember(
        layoutPreferences.gridColType,
        layoutPreferences.gridColMinWidthPct,
        gridSize,
    ) {
        if (layoutPreferences.gridColType != GridColType.ADAPTIVE) {
            return@remember 0.dp
        }
        val availWidth = gridWidthDp - (paddingDp * 2)
        var wDp = availWidth * layoutPreferences.gridColMinWidthPct / 100
        if (wDp <= 0.dp) {
            wDp = 40.dp
        }
        return@remember wDp
    }

    BoxWithConstraints(modifier = modifier) {
        val deviceWidth = remember(maxWidth) {
            var tempWidth = maxWidth / (if (supportsTwoPane) 2 else 3)
            if (maxDeviceHeight > 0.dp) {
                val calcDeviceHeight = tempWidth / deviceAspectRatio
                if (calcDeviceHeight > maxDeviceHeight) {
                    // use maxDeviceHeight to calc tempWidth
                    tempWidth = maxDeviceHeight * deviceAspectRatio
                }
            }
            tempWidth
        }

        Box(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .width(deviceWidth)
                .aspectRatio(deviceAspectRatio)
                .align(Alignment.Center)
                .drawWithCache {
                    onDrawWithContent {
                        drawRoundRect(
                            color = outlineColor,
                            style = Stroke(width = 5f),
                            cornerRadius = CornerRadius(
                                paddingPx * 2,
                                paddingPx * 2,
                            ),
                        )
                        val path = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    top = paddingPx,
                                    bottom = size.height - paddingPx,
                                    left = paddingPx,
                                    right = size.width - paddingPx,
                                    cornerRadius = CornerRadius(
                                        cornerRadiusPx,
                                        cornerRadiusPx,
                                    ),
                                ),
                            )
                        }
                        clipPath(path = path) {
                            this@onDrawWithContent.drawContent()
                        }
                    }
                },
        ) {
            Row {
                LazyVerticalStaggeredGrid(
                    modifier = Modifier
                        .weight(1f)
                        .onSizeChanged { gridSize = it },
                    contentPadding = PaddingValues(paddingDp),
                    columns = when (layoutPreferences.gridColType) {
                        GridColType.ADAPTIVE -> StaggeredGridCells.Adaptive(adaptiveMinWidth)
                        GridColType.FIXED -> StaggeredGridCells.Fixed(
                            layoutPreferences.gridColCount,
                        )
                    },
                    verticalItemSpacing = 4.dp,
                    horizontalArrangement = Arrangement.spacedBy(paddingDp),
                ) {
                    items(resolutions) { resolution ->
                        Box(
                            modifier = Modifier
                                .animateItem()
                                .clip(RoundedCornerShape(corner = CornerSize(cornerRadiusPx)))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant)
                                .fillMaxWidth()
                                .let {
                                    if (layoutPreferences.gridType == GridType.STAGGERED) {
                                        it.aspectRatio(resolution.aspectRatio)
                                    } else {
                                        it.requiredHeight(gridSize.height.toDp() / 3)
                                    }
                                },
                        )
                    }
                }
                if (supportsTwoPane) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        Text(
                            modifier = Modifier
                                .wrapContentHeight()
                                .align(Alignment.Center),
                            text = stringResource(R.string.wallpaper_preview),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

internal class LayoutPreferenceProvider :
    CollectionPreviewParameterProvider<Pair<Boolean, LayoutPreferences>>(
        listOf(
            false to LayoutPreferences(),
            true to LayoutPreferences(),
        ),
    )

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewLayoutPreview(
    @PreviewParameter(LayoutPreferenceProvider::class) twoPaneLayoutPreferences:
    Pair<Boolean, LayoutPreferences>,
) {
    WallFlowTheme {
        Surface {
            LayoutPreview(
                modifier = Modifier.fillMaxWidth(),
                supportsTwoPane = twoPaneLayoutPreferences.first,
                layoutPreferences = twoPaneLayoutPreferences.second,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun LazyListScope.gridTypeSection(
    gridType: GridType = GridType.STAGGERED,
    isExpanded: Boolean = false,
    onGridTypeChange: (GridType) -> Unit = {},
) {
    item {
        SettingsExtraListItem(
            isExpanded = isExpanded,
            headlineContent = {
                Text(text = stringResource(R.string.grid_type))
            },
            trailingContent = {
                val options = GridType.entries.associateWith { getLabelForGridType(it) }
                var expanded by remember { mutableStateOf(false) }

                // TODO: Replace with DropDown composable
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor(
                                type = MenuAnchorType.PrimaryNotEditable,
                            )
                            .fillMaxWidth(0.5f),
                        readOnly = true,
                        value = options[gridType] ?: "",
                        onValueChange = {},
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expanded,
                            )
                        },
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
                                    onGridTypeChange(type)
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewGridTypeSection() {
    WallFlowTheme {
        Surface {
            LazyColumn {
                gridTypeSection()
            }
        }
    }
}

@Composable
private fun getLabelForGridType(gridType: GridType) = when (gridType) {
    GridType.STAGGERED -> stringResource(R.string.staggered)
    GridType.FIXED_SIZE -> stringResource(R.string.fixed_size)
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun LazyListScope.gridColTypeSection(
    gridColType: GridColType = GridColType.ADAPTIVE,
    isExpanded: Boolean = false,
    onGridColTypeChange: (GridColType) -> Unit = {},
) {
    item {
        SettingsExtraListItem(
            isExpanded = isExpanded,
            headlineContent = {
                Text(text = stringResource(R.string.grid_col_type))
            },
            trailingContent = {
                val options = GridColType.entries.associateWith { getLabelForGridColType(it) }
                var expanded by remember { mutableStateOf(false) }

                // TODO: Replace with DropDown composable
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor(
                                type = MenuAnchorType.PrimaryNotEditable,
                            )
                            .fillMaxWidth(0.5f),
                        readOnly = true,
                        value = options[gridColType] ?: "",
                        onValueChange = {},
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expanded,
                            )
                        },
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
                                    onGridColTypeChange(type)
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewGridColTypeSection() {
    WallFlowTheme {
        Surface {
            LazyColumn {
                gridColTypeSection()
            }
        }
    }
}

@Composable
private fun getLabelForGridColType(gridColType: GridColType) = when (gridColType) {
    GridColType.ADAPTIVE -> stringResource(R.string.adaptive)
    GridColType.FIXED -> stringResource(R.string.fixed)
}

internal fun LazyListScope.noOfColumnsSection(
    noOfColumns: Int = 2,
    sliderPadding: Dp = 0.dp,
    isExpanded: Boolean = false,
    onNoOfColumnsChange: (Int) -> Unit = {},
) {
    item {
        val context = LocalContext.current
        var tempCols by remember(noOfColumns) { mutableIntStateOf(noOfColumns) }
        val sliderPosition = tempCols.toFloat()
        SettingsExtraListItem(
            isExpanded = isExpanded,
            headlineContent = {
                Text(text = "${stringResource(R.string.no_of_columns)}: $tempCols")
            },
            supportingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        modifier = Modifier.widthIn(min = sliderPadding),
                        text = MIN_GRID_COLS.toString(),
                        textAlign = TextAlign.Center,
                    )
                    Slider(
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = context.getString(
                                    R.string.no_of_columns,
                                )
                            },
                        value = sliderPosition,
                        onValueChange = { tempCols = it.toInt() },
                        valueRange = MIN_GRID_COLS.toFloat()..MAX_GRID_COLS.toFloat(),
                        onValueChangeFinished = { onNoOfColumnsChange(tempCols) },
                        steps = (MAX_GRID_COLS - MIN_GRID_COLS - 1).toInt(),
                    )
                    Text(
                        modifier = Modifier.widthIn(min = sliderPadding),
                        text = MAX_GRID_COLS.toString(),
                        textAlign = TextAlign.Center,
                    )
                }
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewNoOfColumnsSection() {
    WallFlowTheme {
        Surface {
            LazyColumn {
                noOfColumnsSection()
            }
        }
    }
}

internal fun LazyListScope.adaptiveColMinWidthPctSection(
    minWidthPct: Int = 40,
    sliderPadding: Dp = 0.dp,
    isExpanded: Boolean = false,
    onMinWidthPctChange: (Int) -> Unit = {},
) {
    item {
        val context = LocalContext.current
        var tempPct by remember(minWidthPct) { mutableIntStateOf(minWidthPct) }
        val sliderPosition = tempPct.toFloat()
        SettingsExtraListItem(
            isExpanded = isExpanded,
            headlineContent = {
                Text(text = "${stringResource(R.string.min_col_width)}: $tempPct%")
            },
            supportingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        modifier = Modifier.widthIn(min = sliderPadding),
                        text = MIN_GRID_COL_WIDTH_PCT.toString(),
                        textAlign = TextAlign.Center,
                    )
                    Slider(
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = context.getString(
                                    R.string.no_of_columns,
                                )
                            },
                        value = sliderPosition,
                        onValueChange = { tempPct = it.toInt() },
                        valueRange =
                        MIN_GRID_COL_WIDTH_PCT.toFloat()..MAX_GRID_COL_WIDTH_PCT.toFloat(),
                        onValueChangeFinished = { onMinWidthPctChange(tempPct) },
                        steps = ((MAX_GRID_COL_WIDTH_PCT - MIN_GRID_COL_WIDTH_PCT) / 5 - 1).toInt(),
                    )
                    Text(
                        modifier = Modifier.widthIn(min = sliderPadding),
                        text = MAX_GRID_COL_WIDTH_PCT.toString(),
                        textAlign = TextAlign.Center,
                    )
                }
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAdaptiveColMinWidthPctSection() {
    WallFlowTheme {
        Surface {
            LazyColumn {
                adaptiveColMinWidthPctSection()
            }
        }
    }
}

internal fun LazyListScope.roundedCornersSection(
    roundedCorners: Boolean = true,
    isExpanded: Boolean = false,
    onRoundedCornersChange: (Boolean) -> Unit = {},
) {
    item {
        SettingsExtraListItem(
            isExpanded = isExpanded,
            modifier = Modifier.clickable { onRoundedCornersChange(!roundedCorners) },
            headlineContent = { Text(text = stringResource(R.string.rounded_corners)) },
            trailingContent = {
                Switch(
                    modifier = Modifier.height(24.dp),
                    checked = roundedCorners,
                    onCheckedChange = { onRoundedCornersChange(it) },
                )
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewRoundedCornersSection() {
    WallFlowTheme {
        Surface {
            LazyColumn {
                roundedCornersSection()
            }
        }
    }
}
