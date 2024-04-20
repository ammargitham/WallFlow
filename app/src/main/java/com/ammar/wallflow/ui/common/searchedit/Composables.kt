package com.ammar.wallflow.ui.common.searchedit

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.COMMON_RESOLUTIONS
import com.ammar.wallflow.R
import com.ammar.wallflow.extensions.getScreenResolution
import com.ammar.wallflow.extensions.trimAll
import com.ammar.wallflow.model.Order
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.RedditSort
import com.ammar.wallflow.model.search.RedditTimeRange
import com.ammar.wallflow.model.search.SavedSearch
import com.ammar.wallflow.model.search.WallhavenCategory
import com.ammar.wallflow.model.search.WallhavenRatio
import com.ammar.wallflow.model.search.WallhavenRatio.CategoryWallhavenRatio
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.WallhavenSorting
import com.ammar.wallflow.model.search.WallhavenTopRange
import com.ammar.wallflow.ui.common.ClearableChip
import com.ammar.wallflow.ui.common.IntState
import com.ammar.wallflow.ui.common.drawVerticalScrollbar
import com.ammar.wallflow.ui.common.intStateSaver
import com.ammar.wallflow.ui.common.taginput.TagInputField
import com.ammar.wallflow.ui.theme.WallFlowTheme
import kotlinx.coroutines.launch

@Composable
internal fun IncludedTagsFilter(
    modifier: Modifier = Modifier,
    tags: Set<String> = emptySet(),
    onChange: (tags: Set<String>) -> Unit = {},
) {
    TagInputField(
        modifier = modifier,
        tags = tags,
        label = { Text(text = stringResource(R.string.included_tags_keywords)) },
        onAddTag = { onChange(tags + it) },
        onRemoveTag = { onChange(tags - it) },
        tagFromInputString = { it },
    )
}

@Composable
internal fun ExcludedTagsFilter(
    modifier: Modifier = Modifier,
    tags: Set<String> = emptySet(),
    onChange: (tags: Set<String>) -> Unit = {},
) {
    TagInputField(
        modifier = modifier,
        label = { Text(text = stringResource(R.string.excluded_tags_keywords)) },
        tags = tags,
        onAddTag = { onChange(tags + it) },
        onRemoveTag = { onChange(tags - it) },
        tagFromInputString = { it },
    )
}

@Composable
internal fun CategoriesFilter(
    categories: Set<WallhavenCategory> = setOf(WallhavenCategory.PEOPLE),
    onChange: (categories: Set<WallhavenCategory>) -> Unit = {},
) {
    Column {
        Text(
            text = stringResource(R.string.categories),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.requiredHeight(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WallhavenCategory.entries.map {
                val selected = it in categories

                FilterChip(
                    label = { Text(text = getCategoryString(it)) },
                    leadingIcon = {
                        AnimatedVisibility(selected) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                            )
                        }
                    },
                    selected = selected,
                    onClick = {
                        onChange(
                            if (selected && categories.size > 1) {
                                categories - it
                            } else {
                                categories + it
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
internal fun PurityFilter(
    purities: Set<Purity> = setOf(Purity.SFW),
    showNSFW: Boolean = false,
    showSketchy: Boolean = true,
    canToggleSFW: Boolean = true,
    onChange: (purities: Set<Purity>) -> Unit = {},
) {
    Column {
        Text(
            text = stringResource(R.string.purity),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.requiredHeight(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Purity.entries.filter {
                when (it) {
                    Purity.SFW -> true
                    Purity.SKETCHY -> showSketchy
                    Purity.NSFW -> showNSFW
                }
            }.map {
                val selected = it in purities

                FilterChip(
                    label = { Text(text = getPurityString(it)) },
                    leadingIcon = {
                        AnimatedVisibility(selected) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                            )
                        }
                    },
                    selected = selected,
                    enabled = if (it == Purity.SFW) {
                        canToggleSFW
                    } else {
                        true
                    },
                    onClick = {
                        onChange(
                            if (selected && purities.size > 1) purities - it else purities + it,
                        )
                    },
                )
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPurityFilter() {
    WallFlowTheme {
        Surface {
            Column {
                PurityFilter()
                PurityFilter(showNSFW = true)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WallhavenSortingFilter(
    sorting: WallhavenSorting = WallhavenSorting.DATE_ADDED,
    onChange: (sorting: WallhavenSorting) -> Unit = {},
) {
    Column {
        Text(
            text = stringResource(R.string.sorting),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.requiredHeight(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WallhavenSorting.entries.map {
                val selected = it == sorting

                FilterChip(
                    label = { Text(text = getWallhavenSortingString(it)) },
                    leadingIcon = {
                        AnimatedVisibility(selected) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                            )
                        }
                    },
                    selected = selected,
                    onClick = { onChange(it) },
                )
            }
        }
    }
}

@OptIn(
    ExperimentalLayoutApi::class,
)
@Composable
internal fun RedditSortFilter(
    sort: RedditSort = RedditSort.RELEVANCE,
    onChange: (RedditSort) -> Unit = {},
) {
    Column {
        Text(
            text = stringResource(R.string.sort),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.requiredHeight(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RedditSort.entries.map {
                val selected = it == sort

                FilterChip(
                    label = { Text(text = getRedditSortString(it)) },
                    leadingIcon = {
                        AnimatedVisibility(selected) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                            )
                        }
                    },
                    selected = selected,
                    onClick = { onChange(it) },
                )
            }
        }
    }
}

@OptIn(
    ExperimentalLayoutApi::class,
)
@Composable
internal fun TopRangeFilter(
    topRange: WallhavenTopRange = WallhavenTopRange.ONE_MONTH,
    onChange: (topRange: WallhavenTopRange) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.top_range),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.requiredHeight(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WallhavenTopRange.entries.map {
                val selected = it == topRange

                FilterChip(
                    label = { Text(text = getTopRangeString(it)) },
                    leadingIcon = {
                        AnimatedVisibility(selected) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                            )
                        }
                    },
                    selected = selected,
                    onClick = { onChange(it) },
                )
            }
        }
    }
}

@OptIn(
    ExperimentalLayoutApi::class,
)
@Composable
internal fun RedditTimeRangeFilter(
    timeRange: RedditTimeRange = RedditTimeRange.ALL,
    onChange: (RedditTimeRange) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.posts_from),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.requiredHeight(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RedditTimeRange.entries.map {
                val selected = it == timeRange

                FilterChip(
                    label = { Text(text = getRedditTimeRangeString(it)) },
                    leadingIcon = {
                        AnimatedVisibility(selected) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                            )
                        }
                    },
                    selected = selected,
                    onClick = { onChange(it) },
                )
            }
        }
    }
}

@Composable
internal fun OrderFilter(
    order: Order = Order.DESC,
    onChange: (order: Order) -> Unit = {},
) {
    Column {
        Text(
            text = stringResource(R.string.order),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.requiredHeight(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Order.entries.map {
                val selected = it == order

                FilterChip(
                    label = { Text(text = getOrderString(it)) },
                    leadingIcon = {
                        Crossfade(
                            targetState = selected,
                            label = "leadingIconCrossfade",
                        ) { isSelected ->
                            if (isSelected) {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                )
                            } else {
                                when (it) {
                                    Order.DESC -> Icon(
                                        modifier = Modifier.size(16.dp),
                                        painter = painterResource(
                                            R.drawable.baseline_sort_descending_24,
                                        ),
                                        contentDescription = null,
                                    )

                                    Order.ASC -> Icon(
                                        modifier = Modifier.size(16.dp),
                                        painter = painterResource(
                                            R.drawable.baseline_sort_ascending_24,
                                        ),
                                        contentDescription = null,
                                    )
                                }
                            }
                        }
                    },
                    selected = selected,
                    onClick = { onChange(it) },
                )
            }
        }
    }
}

@Composable
internal fun MinResolutionFilter(
    modifier: Modifier = Modifier,
    resolution: IntSize? = null,
    onChange: (IntSize?) -> Unit = {},
    onAddCustomResolutionClick: () -> Unit = {},
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.min_resolution),
            style = MaterialTheme.typography.labelLarge,
        )
        if (resolution != null) {
            ClearableChip(
                label = { Text(resolution.toString()) },
                onClear = { onChange(null) },
            )
        } else {
            AddResolutionButton(
                addedResolutions = emptySet(),
                onAdd = { onChange(it) },
                onCustomClick = onAddCustomResolutionClick,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ResolutionsFilter(
    modifier: Modifier = Modifier,
    resolutions: Set<IntSize> = emptySet(),
    onChange: (resolutions: Set<IntSize>) -> Unit = {},
    onAddCustomResolutionClick: () -> Unit = {},
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.resolutions),
            style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (resolutions.isEmpty()) {
                ClearableChip(
                    label = { Text(text = stringResource(R.string.any)) },
                    showClearIcon = false,
                )
            }

            resolutions.map {
                ClearableChip(
                    label = { Text(it.toString()) },
                    onClear = { onChange(resolutions - it) },
                )
            }
        }
        AddResolutionButton(
            addedResolutions = resolutions,
            onAdd = { onChange(resolutions + it) },
            onCustomClick = onAddCustomResolutionClick,
        )
    }
}

@Composable
private fun AddResolutionButton(
    modifier: Modifier = Modifier,
    addedResolutions: Set<IntSize> = emptySet(),
    onAdd: (resolution: IntSize) -> Unit = {},
    onCustomClick: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val localResolution = context.getScreenResolution(true)
    val localInCommon = remember(localResolution) { localResolution in COMMON_RESOLUTIONS.values }
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(Alignment.Top),
    ) {
        FilledTonalButton(
            onClick = { expanded = true },
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            Icon(
                modifier = Modifier.size(ButtonDefaults.IconSize),
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.add_resolution),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(R.string.add_resolution))
        }
        DropdownMenu(
            modifier = Modifier
                .heightIn(max = 300.dp)
                .drawVerticalScrollbar(
                    state = scrollState,
                    initiallyVisible = true,
                ),
            expanded = expanded,
            scrollState = scrollState,
            onDismissRequest = { expanded = false },
        ) {
            if (!localInCommon && localResolution !in addedResolutions) {
                // if local device resolution is not in COMMON_RESOLUTIONS, add an entry
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(
                                R.string.current_resolution,
                                localResolution.toString(),
                            ),
                        )
                    },
                    onClick = { onAdd(localResolution) },
                )
                HorizontalDivider()
            }
            COMMON_RESOLUTIONS.entries
                .filter { it.value !in addedResolutions }
                .map {
                    val text = "${it.key} ${
                        // if this resolution is same as local, add Current label
                        if (it.value == localResolution) {
                            "(${stringResource(R.string.current)})"
                        } else {
                            ""
                        }
                    } (${it.value})"

                    DropdownMenuItem(
                        text = { Text(text = text) },
                        onClick = {
                            expanded = false
                            onAdd(it.value)
                        },
                    )
                    HorizontalDivider()
                }
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.custom)) },
                onClick = {
                    onCustomClick()
                    expanded = false
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RatioFilter(
    modifier: Modifier = Modifier,
    ratios: Set<WallhavenRatio> = emptySet(),
    onChange: (Set<WallhavenRatio>) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = modifier.fillMaxWidth(),
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        TagInputField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    type = MenuAnchorType.PrimaryNotEditable,
                ),
            readOnly = true,
            tags = ratios,
            showTagClearAction = false,
            onAddTag = {},
            onRemoveTag = { onChange(ratios - it) },
            label = { Text(text = stringResource(R.string.ratio)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            getTagString = { it.toRatioString().capitalize(Locale.current) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            RatioMenuContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                selectedRatios = ratios,
                onOptionClick = {
                    onChange(
                        if (it in ratios) {
                            ratios - it
                        } else {
                            ratios + it
                        },
                    )
                    expanded = false
                },
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewRatioFilter() {
    WallFlowTheme {
        Surface {
            RatioFilter()
        }
    }
}

private data class RatioOption(
    val ratio: WallhavenRatio? = null,
    val span: Int,
)

private val ratioOptions = listOf(
    (16 to 9) to 1,
    (21 to 9) to 1,
    (9 to 16) to 1,
    (1 to 1) to 1,
    (16 to 10) to 1,
    (32 to 9) to 1,
    (10 to 16) to 1,
    (3 to 2) to 1,
    null to 1,
    (48 to 9) to 1,
    (9 to 18) to 1,
    (4 to 3) to 1,
    null to 3,
    (5 to 4) to 1,
).map {
    RatioOption(
        ratio = it.first?.let { sizePair ->
            WallhavenRatio.fromSize(IntSize(sizePair.first, sizePair.second))
        },
        span = it.second,
    )
}

private fun getRatioGridHeaders(context: Context) = listOf(
    context.getString(R.string.wide),
    context.getString(R.string.ultrawide),
    context.getString(R.string.portrait),
    context.getString(R.string.square),
)

@Composable
private fun RatioMenuContent(
    modifier: Modifier = Modifier,
    selectedRatios: Set<WallhavenRatio> = emptySet(),
    onOptionClick: (WallhavenRatio) -> Unit = {},
) {
    val context = LocalContext.current

    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val landscapeRatio = WallhavenRatio.fromCategory(
                CategoryWallhavenRatio.Category.LANDSCAPE,
            )
            val portraitRatio = WallhavenRatio.fromCategory(
                CategoryWallhavenRatio.Category.PORTRAIT,
            )

            LandscapeChip(
                selected = landscapeRatio in selectedRatios,
                onClick = { onOptionClick(landscapeRatio) },
            )
            PortraitChip(
                selected = portraitRatio in selectedRatios,
                onClick = { onOptionClick(portraitRatio) },
            )
        }
        Spacer(modifier = Modifier.requiredHeight(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            getRatioGridHeaders(context).forEach {
                GridHeader(
                    modifier = Modifier.weight(1f),
                    text = it,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        RatioOptionGrid(
            modifier = Modifier.fillMaxWidth(),
            selectedRatios = selectedRatios,
            onOptionClick = onOptionClick,
        )
    }
}

@Composable
private fun GridHeader(
    modifier: Modifier = Modifier,
    text: String,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@Composable
private fun RatioOptionGrid(
    modifier: Modifier = Modifier,
    selectedRatios: Set<WallhavenRatio> = emptySet(),
    onOptionClick: (WallhavenRatio) -> Unit = {},
) {
    Column(
        modifier = modifier,
    ) {
        ratioOptions.chunked(4).forEach { chunk ->
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                chunk.forEach {
                    Box(
                        modifier = Modifier.weight(1f * it.span),
                    ) {
                        if (it.ratio != null) {
                            FilterChip(
                                modifier = Modifier.align(Alignment.Center),
                                label = { Text(text = it.ratio.toRatioString()) },
                                selected = it.ratio in selectedRatios,
                                onClick = { onOptionClick(it.ratio) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LandscapeChip(
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    FilterChip(
        label = { Text(text = stringResource(R.string.landscape)) },
        leadingIcon = {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(R.drawable.baseline_crop_landscape_24),
                contentDescription = null,
            )
        },
        selected = selected,
        onClick = onClick,
    )
}

@Composable
private fun PortraitChip(
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    FilterChip(
        label = { Text(text = stringResource(R.string.portrait)) },
        leadingIcon = {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(R.drawable.baseline_crop_portrait_24),
                contentDescription = null,
            )
        },
        selected = selected,
        onClick = onClick,
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewRatioMenuContent() {
    WallFlowTheme {
        Surface {
            RatioMenuContent()
        }
    }
}

@Composable
private fun getCategoryString(category: WallhavenCategory) = stringResource(
    when (category) {
        WallhavenCategory.GENERAL -> R.string.general
        WallhavenCategory.ANIME -> R.string.anime
        WallhavenCategory.PEOPLE -> R.string.people
    },
)

@Composable
private fun getPurityString(purity: Purity) = stringResource(
    when (purity) {
        Purity.SFW -> R.string.sfw
        Purity.SKETCHY -> R.string.sketchy
        Purity.NSFW -> R.string.nsfw
    },
)

@Composable
private fun getWallhavenSortingString(sorting: WallhavenSorting) = stringResource(
    when (sorting) {
        WallhavenSorting.DATE_ADDED -> R.string.date_added
        WallhavenSorting.RELEVANCE -> R.string.relevance
        WallhavenSorting.RANDOM -> R.string.random
        WallhavenSorting.VIEWS -> R.string.views
        WallhavenSorting.FAVORITES -> R.string.favorites
        WallhavenSorting.TOPLIST -> R.string.top
    },
)

@Composable
private fun getRedditSortString(sort: RedditSort) = stringResource(
    when (sort) {
        RedditSort.TOP -> R.string.top
        RedditSort.RELEVANCE -> R.string.relevance
        RedditSort.NEW -> R.string.sort_new
        RedditSort.COMMENTS -> R.string.comments
    },
)

@Composable
private fun getRedditTimeRangeString(timeRange: RedditTimeRange) = stringResource(
    when (timeRange) {
        RedditTimeRange.ALL -> R.string.all
        RedditTimeRange.HOUR -> R.string.one_hour
        RedditTimeRange.DAY -> R.string.one_day
        RedditTimeRange.MONTH -> R.string.one_month
        RedditTimeRange.WEEK -> R.string.one_week
        RedditTimeRange.YEAR -> R.string.one_year
    },
)

@Composable
private fun getTopRangeString(topRange: WallhavenTopRange) = stringResource(
    when (topRange) {
        WallhavenTopRange.ONE_DAY -> R.string.one_day
        WallhavenTopRange.THREE_DAYS -> R.string.three_days
        WallhavenTopRange.ONE_WEEK -> R.string.one_week
        WallhavenTopRange.ONE_MONTH -> R.string.one_month
        WallhavenTopRange.THREE_MONTHS -> R.string.three_months
        WallhavenTopRange.SIX_MONTHS -> R.string.six_months
        WallhavenTopRange.ONE_YEAR -> R.string.one_year
    },
)

@Composable
private fun getOrderString(order: Order) = stringResource(
    when (order) {
        Order.DESC -> R.string.descending
        Order.ASC -> R.string.ascending
    },
)

@Composable
fun SaveAsDialog(
    modifier: Modifier = Modifier,
    checkNameExists: suspend (name: String) -> Boolean = { false },
    onSave: (String) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var isError by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.save_as)) },
        text = {
            OutlinedTextField(
                modifier = Modifier.focusRequester(focusRequester),
                label = { Text(text = stringResource(R.string.name)) },
                value = name,
                isError = isError,
                supportingText = if (isError) {
                    {
                        Text(
                            text = stringResource(
                                if (name.isBlank()) {
                                    R.string.name_cannot_be_empty
                                } else {
                                    R.string.name_already_used
                                },
                            ),
                        )
                    }
                } else {
                    null
                },
                onValueChange = {
                    name = it
                    if (it.isBlank()) {
                        isError = true
                        return@OutlinedTextField
                    }
                    coroutineScope.launch {
                        isError = checkNameExists(name.trimAll())
                    }
                },
            )
        },
        confirmButton = {
            TextButton(
                enabled = !isError,
                onClick = { onSave(name.trimAll()) },
            ) {
                Text(text = stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        onDismissRequest = onDismissRequest,
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSaveAsDialog() {
    WallFlowTheme {
        SaveAsDialog()
    }
}

@Composable
fun SavedSearchesDialog(
    modifier: Modifier = Modifier,
    savedSearches: List<SavedSearch> = emptyList(),
    title: String = stringResource(R.string.load_search),
    selectable: Boolean = true,
    showActions: Boolean = false,
    onSelect: (SavedSearch) -> Unit = {},
    onDismissRequest: () -> Unit = {},
    onEditClick: (SavedSearch) -> Unit = {},
    onDeleteClick: (SavedSearch) -> Unit = {},
) {
    AlertDialog(
        modifier = modifier,
        title = { Text(text = title) },
        text = {
            LazyColumn {
                if (savedSearches.isEmpty()) {
                    item {
                        Text(text = stringResource(R.string.no_saved_searches))
                    }
                    return@LazyColumn
                }
                items(savedSearches) {
                    SavedSearchItem(
                        modifier = Modifier.clickable(enabled = selectable) {
                            onSelect(it)
                        },
                        savedSearch = it,
                        showActions = showActions,
                        onEditClick = onEditClick,
                        onDeleteClick = onDeleteClick,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun SavedSearchItem(
    modifier: Modifier = Modifier,
    savedSearch: SavedSearch,
    showActions: Boolean,
    onEditClick: (SavedSearch) -> Unit,
    onDeleteClick: (SavedSearch) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(
                when (savedSearch.search) {
                    is WallhavenSearch -> R.drawable.wallhaven_logo_short
                    is RedditSearch -> R.drawable.reddit
                },
            ),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.requiredWidth(16.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = savedSearch.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (showActions) {
            IconButton(onClick = { onEditClick(savedSearch) }) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit),
                )
            }
            IconButton(onClick = { onDeleteClick(savedSearch) }) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                )
            }
        }
    }
}

private val tempSavedSearches = List(3) {
    SavedSearch(
        name = "Saved search $it",
        search = WallhavenSearch(),
    )
}

private class SavedSearchesDialogPreviewParameterProvider :
    CollectionPreviewParameterProvider<Pair<List<SavedSearch>, Boolean>>(
        listOf(
            Pair(emptyList(), false),
            Pair(tempSavedSearches, true),
            Pair(tempSavedSearches, false),
        ),
    )

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSavedSearchesDialog(
    @PreviewParameter(SavedSearchesDialogPreviewParameterProvider::class) parameters:
    Pair<List<SavedSearch>, Boolean>,
) {
    WallFlowTheme {
        SavedSearchesDialog(
            savedSearches = parameters.first,
            showActions = parameters.second,
        )
    }
}

@Composable
fun CustomResolutionDialog(
    modifier: Modifier = Modifier,
    resolution: IntSize? = null,
    onSave: (IntSize) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val context = LocalContext.current

    fun getValidationError(
        value: String,
        @StringRes blankError: Int,
        @StringRes invalidValueError: Int,
    ): String {
        if (value.isBlank()) {
            return context.getString(blankError)
        }
        val intVal = value.toIntOrNull()
        return if (intVal == null || intVal <= 0) {
            context.getString(invalidValueError)
        } else {
            ""
        }
    }

    val widthState by rememberSaveable(
        stateSaver = intStateSaver {
            getValidationError(it, R.string.width_cannot_be_blank, R.string.invalid_width)
        },
    ) { mutableStateOf(IntState(value = resolution?.width)) }
    val heightState by rememberSaveable(
        stateSaver = intStateSaver {
            getValidationError(it, R.string.height_cannot_be_blank, R.string.invalid_height)
        },
    ) { mutableStateOf(IntState(value = resolution?.height)) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.custom_resolution)) },
        text = {
            val focusManager = LocalFocusManager.current
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            widthState.onFocusChange(focusState.isFocused)
                            if (!focusState.isFocused) {
                                widthState.enableShowErrors()
                            }
                        },
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) },
                    ),
                    label = { Text(text = stringResource(R.string.width)) },
                    value = widthState.text,
                    onValueChange = {
                        widthState.text = it
                        widthState.enableShowErrors()
                    },
                )
                Text(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    text = "x",
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            heightState.onFocusChange(focusState.isFocused)
                            if (!focusState.isFocused) {
                                heightState.enableShowErrors()
                            }
                        },
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!widthState.isValid || !heightState.isValid) {
                                return@KeyboardActions
                            }
                            onSave(IntSize(widthState.text.toInt(), heightState.text.toInt()))
                        },
                    ),
                    label = { Text(text = stringResource(R.string.height)) },
                    value = heightState.text,
                    onValueChange = {
                        heightState.text = it
                        heightState.enableShowErrors()
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = widthState.isValid && heightState.isValid,
                onClick = { onSave(IntSize(widthState.text.toInt(), heightState.text.toInt())) },
            ) {
                Text(text = stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        onDismissRequest = onDismissRequest,
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCustomResolutionDialog() {
    WallFlowTheme {
        CustomResolutionDialog()
    }
}
