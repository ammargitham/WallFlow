package com.ammar.havenwalls.ui.common.searchedit

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.havenwalls.R
import com.ammar.havenwalls.model.SavedSearch
import com.ammar.havenwalls.model.Search
import com.ammar.havenwalls.ui.theme.HavenWallsTheme

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
