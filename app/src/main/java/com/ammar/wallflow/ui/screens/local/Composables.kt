package com.ammar.wallflow.ui.screens.local

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.model.local.LocalDirectory
import com.ammar.wallflow.ui.common.AdaptiveBottomSheet
import com.ammar.wallflow.ui.common.AdaptiveBottomSheetState
import com.ammar.wallflow.ui.common.rememberAdaptiveBottomSheetState
import com.ammar.wallflow.ui.theme.WallFlowTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManageFoldersBottomSheet(
    modifier: Modifier = Modifier,
    state: AdaptiveBottomSheetState = rememberAdaptiveBottomSheetState(),
    folders: List<LocalDirectory> = emptyList(),
    sort: LocalSort = LocalSort.NO_SORT,
    onDismissRequest: () -> Unit = {},
    onAddFolderClick: () -> Unit = {},
    onRemoveClick: (LocalDirectory) -> Unit = {},
    onSortChange: (LocalSort) -> Unit = {},
) {
    AdaptiveBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = state,
    ) {
        ManageFoldersSheetContent(
            modifier = Modifier.fillMaxSize(),
            folders = folders,
            contentPadding = PaddingValues(
                bottom = 44.dp,
            ),
            sort = sort,
            onAddFolderClick = onAddFolderClick,
            onRemoveClick = onRemoveClick,
            onSortChange = onSortChange,
        )
    }
}

@Composable
private fun ManageFoldersSheetContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    folders: List<LocalDirectory> = emptyList(),
    sort: LocalSort = LocalSort.NO_SORT,
    onAddFolderClick: () -> Unit = {},
    onRemoveClick: (LocalDirectory) -> Unit = {},
    onSortChange: (LocalSort) -> Unit = {},
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            modifier = Modifier.padding(
                start = 22.dp,
                end = 22.dp,
                bottom = 16.dp,
            ),
            text = stringResource(R.string.manage_local),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.headlineMedium,
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        LazyColumn(
            modifier = Modifier,
            contentPadding = contentPadding,
        ) {
            item {
                Sort(
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
                    sort = sort,
                    onChange = onSortChange,
                )
            }
            item {
                ListItem(
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.dirs),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    trailingContent = {
                        FilledTonalButton(
                            onClick = onAddFolderClick,
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                        ) {
                            Text(text = stringResource(R.string.add))
                        }
                    },
                )
            }
            items(folders) {
                ListItem(
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    headlineContent = {
                        Text(text = it.path)
                    },
                    trailingContent = {
                        IconButton(
                            onClick = {
                                onRemoveClick(it)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.remove),
                            )
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Sort(
    modifier: Modifier = Modifier,
    sort: LocalSort = LocalSort.NO_SORT,
    onChange: (sort: LocalSort) -> Unit = {},
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.sort),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.requiredHeight(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LocalSort.entries.map {
                val selected = it == sort

                FilterChip(
                    label = { Text(text = getSortString(it)) },
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
fun getSortString(sort: LocalSort) = when (sort) {
    LocalSort.NO_SORT -> stringResource(R.string.no_sort)
    LocalSort.NAME -> stringResource(R.string.name)
    LocalSort.LAST_MODIFIED -> stringResource(R.string.last_modified)
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewManageFoldersSheetContent() {
    WallFlowTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            ManageFoldersSheetContent(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = 44.dp,
                ),
                folders = listOf(
                    LocalDirectory(
                        uri = Uri.EMPTY,
                        path = "/test/test1/test2",
                    ),
                ),
            )
        }
    }
}
