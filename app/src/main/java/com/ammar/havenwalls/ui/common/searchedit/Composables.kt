package com.ammar.havenwalls.ui.common.searchedit

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.havenwalls.COMMON_RESOLUTIONS
import com.ammar.havenwalls.R
import com.ammar.havenwalls.extensions.toPx
import com.ammar.havenwalls.model.Category
import com.ammar.havenwalls.model.Order
import com.ammar.havenwalls.model.Purity
import com.ammar.havenwalls.model.Resolution
import com.ammar.havenwalls.model.SavedSearch
import com.ammar.havenwalls.model.Search
import com.ammar.havenwalls.model.Sorting
import com.ammar.havenwalls.model.TopRange
import com.ammar.havenwalls.ui.common.ClearableChip
import com.ammar.havenwalls.ui.common.taginput.TagInputField
import com.ammar.havenwalls.ui.theme.HavenWallsTheme

@Composable
internal fun IncludedTagsFilter(
    modifier: Modifier = Modifier,
    tags: Set<String> = emptySet(),
    onChange: (tags: Set<String>) -> Unit = {},
) {
    TagInputField(
        modifier = modifier,
        tags = tags,
        label = { Text(text = "Included Tags/Keywords") },
        onAddTag = { onChange(tags + it) },
        onRemoveTag = { onChange(tags - it) },
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
        label = { Text(text = "Excluded Tags/Keywords") },
        tags = tags,
        onAddTag = { onChange(tags + it) },
        onRemoveTag = { onChange(tags - it) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoriesFilter(
    categories: Set<Category> = setOf(Category.PEOPLE),
    onChange: (categories: Set<Category>) -> Unit = {},
) {
    Column {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.requiredHeight(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Category.values().map {
                val selected = it in categories

                FilterChip(
                    label = { Text(text = getCategoryString(it)) },
                    leadingIcon = {
                        AnimatedVisibility(selected) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Default.Check,
                                contentDescription = null
                            )
                        }
                    },
                    selected = selected,
                    onClick = { onChange(if (selected && categories.size > 1) categories - it else categories + it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PurityFilter(
    purities: Set<Purity> = setOf(Purity.SFW),
    onChange: (purities: Set<Purity>) -> Unit = {},
) {
    Column {
        Text(
            text = "Purity",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.requiredHeight(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Purity.values().map {
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
                    onClick = { onChange(if (selected && purities.size > 1) purities - it else purities + it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SortingFilter(
    sorting: Sorting = Sorting.DATE_ADDED,
    onChange: (sorting: Sorting) -> Unit = {},
) {
    Column {
        Text(
            text = "Sorting",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.requiredHeight(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Sorting.values().map {
                val selected = it == sorting

                FilterChip(
                    label = { Text(text = getSortingString(it)) },
                    leadingIcon = {
                        AnimatedVisibility(selected) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Default.Check,
                                contentDescription = null
                            )
                        }
                    },
                    selected = selected,
                    onClick = { onChange(it) }
                )
            }
        }
    }
}

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
internal fun TopRangeFilter(
    topRange: TopRange = TopRange.ONE_MONTH,
    onChange: (topRange: TopRange) -> Unit,
) {
    Column {
        Text(
            text = "Top Range",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.requiredHeight(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TopRange.values().map {
                val selected = it == topRange

                FilterChip(
                    label = { Text(text = getTopRangeString(it)) },
                    leadingIcon = {
                        AnimatedVisibility(selected) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Default.Check,
                                contentDescription = null
                            )
                        }
                    },
                    selected = selected,
                    onClick = { onChange(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrderFilter(
    order: Order = Order.DESC,
    onChange: (order: Order) -> Unit = {},
) {
    Column {
        Text(
            text = "Order",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.requiredHeight(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Order.values().map {
                val selected = it == order

                FilterChip(
                    label = { Text(text = getOrderString(it)) },
                    leadingIcon = {
                        Crossfade(selected) { isSelected ->
                            if (isSelected) {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null
                                )
                            } else {
                                when (it) {
                                    Order.DESC -> Icon(
                                        modifier = Modifier.size(16.dp),
                                        painter = painterResource(R.drawable.baseline_sort_descending_24),
                                        contentDescription = null
                                    )

                                    Order.ASC -> Icon(
                                        modifier = Modifier.size(16.dp),
                                        painter = painterResource(R.drawable.baseline_sort_ascending_24),
                                        contentDescription = null
                                    )
                                }
                            }
                        }

                    },
                    selected = selected,
                    onClick = { onChange(it) }
                )
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ResolutionsFilter(
    resolutions: Set<Resolution> = emptySet(),
    onChange: (resolutions: Set<Resolution>) -> Unit = {},
    onAddCustomResolutionClick: () -> Unit = {},
) {
    Column {
        Text(
            text = "Resolutions",
            style = MaterialTheme.typography.labelLarge
        )
        FlowRow(
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (resolutions.isEmpty()) {
                ClearableChip(
                    label = { Text("Any") },
                    showClearIcon = false,
                )
            }

            resolutions.map {
                ClearableChip(
                    label = { Text(it.toString()) },
                    onClear = { onChange(resolutions - it) }
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
    addedResolutions: Set<Resolution> = emptySet(),
    onAdd: (resolution: Resolution) -> Unit = {},
    onCustomClick: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp.toPx()
    val screenWidth = configuration.screenWidthDp.dp.toPx()
    val localResolution = remember(screenHeight, screenWidth) {
        Resolution(screenWidth, screenHeight)
    }
    val localInCommon = remember(localResolution) { localResolution in COMMON_RESOLUTIONS.values }

    Box(
        modifier = modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.TopStart),
    ) {
        FilledTonalButton(
            onClick = { expanded = true },
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            Icon(
                modifier = Modifier.size(ButtonDefaults.IconSize),
                imageVector = Icons.Outlined.Add,
                contentDescription = "Add resolution",
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = "Add resolution")
        }
        DropdownMenu(
            modifier = Modifier.heightIn(max = 300.dp),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (!localInCommon && localResolution !in addedResolutions) {
                // if local device resolution is not in COMMON_RESOLUTIONS, add an entry
                DropdownMenuItem(
                    text = { Text(text = "Current ($localResolution)") },
                    onClick = { onAdd(localResolution) },
                )
            }
            COMMON_RESOLUTIONS.entries
                .filter { it.value !in addedResolutions }
                .map {
                    // if this resolution is same as local, add Current label
                    val text = "${it.key} ${
                        if (it.value == localResolution) "(Current)" else ""
                    } (${it.value})"

                    DropdownMenuItem(
                        text = { Text(text = text) },
                        onClick = {
                            expanded = false
                            onAdd(it.value)
                        },
                    )
                    Divider()
                }
            DropdownMenuItem(
                text = { Text(text = "Custom...") },
                onClick = onCustomClick,
            )
        }
    }
}

@Composable
private fun getCategoryString(category: Category) = stringResource(
    when (category) {
        Category.GENERAL -> R.string.general
        Category.ANIME -> R.string.anime
        Category.PEOPLE -> R.string.people
    }
)

@Composable
private fun getPurityString(purity: Purity) = stringResource(
    when (purity) {
        Purity.SFW -> R.string.sfw
        Purity.SKETCHY -> R.string.sketchy
        Purity.NSFW -> R.string.nsfw
    }
)

@Composable
private fun getSortingString(sorting: Sorting) = stringResource(
    when (sorting) {
        Sorting.DATE_ADDED -> R.string.date_added
        Sorting.RELEVANCE -> R.string.relevance
        Sorting.RANDOM -> R.string.random
        Sorting.VIEWS -> R.string.views
        Sorting.FAVORITES -> R.string.favorites
        Sorting.TOPLIST -> R.string.top
    }
)

@Composable
private fun getTopRangeString(topRange: TopRange) = stringResource(
    when (topRange) {
        TopRange.ONE_DAY -> R.string.one_day
        TopRange.THREE_DAYS -> R.string.three_days
        TopRange.ONE_WEEK -> R.string.one_week
        TopRange.ONE_MONTH -> R.string.one_month
        TopRange.THREE_MONTHS -> R.string.three_months
        TopRange.SIX_MONTHS -> R.string.six_months
        TopRange.ONE_YEAR -> R.string.one_year
    }
)

@Composable
private fun getOrderString(order: Order) = stringResource(
    when (order) {
        Order.DESC -> R.string.descending
        Order.ASC -> R.string.ascending
    }
)

@Composable
fun SaveAsDialog(
    modifier: Modifier = Modifier,
    onSave: (String) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val context = LocalContext.current
    val nameState by rememberSaveable(stateSaver = nameStateSaver(context)) {
        mutableStateOf(NameState(context, ""))
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.save_as)) },
        text = {
            OutlinedTextField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        nameState.onFocusChange(focusState.isFocused)
                        if (!focusState.isFocused) {
                            nameState.enableShowErrors()
                        }
                    },
                label = { Text(text = stringResource(R.string.name)) },
                value = nameState.text,
                onValueChange = {
                    nameState.text = it
                    nameState.enableShowErrors()
                },
            )
        },
        confirmButton = {
            TextButton(
                enabled = nameState.isValid,
                onClick = { onSave(nameState.text) },
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

@Preview(showSystemUi = true)
@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSaveAsDialog() {
    HavenWallsTheme {
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
                        onDeleteClick = onDeleteClick
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
        search = Search(),
    )
}

private class SavedSearchesDialogPreviewParameterProvider :
    CollectionPreviewParameterProvider<Pair<List<SavedSearch>, Boolean>>(
        listOf(
            Pair(emptyList(), false),
            Pair(tempSavedSearches, true),
            Pair(tempSavedSearches, false),
        )
    )

@Preview(showSystemUi = true)
@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSavedSearchesDialog(
    @PreviewParameter(SavedSearchesDialogPreviewParameterProvider::class) parameters: Pair<List<SavedSearch>, Boolean>,
) {
    HavenWallsTheme {
        SavedSearchesDialog(
            savedSearches = parameters.first,
            showActions = parameters.second,
        )
    }
}
