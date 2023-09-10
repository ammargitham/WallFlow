package com.ammar.wallflow.ui.screens.local

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.model.local.LocalDirectory
import com.ammar.wallflow.ui.theme.WallFlowTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManageFoldersBottomSheet(
    modifier: Modifier = Modifier,
    state: SheetState = rememberModalBottomSheetState(),
    folders: List<LocalDirectory> = emptyList(),
    onDismissRequest: () -> Unit = {},
    onAddFolderClick: () -> Unit = {},
    onRemoveClick: (LocalDirectory) -> Unit = {},
) {
    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = state,
    ) {
        ManageFoldersSheetContent(
            modifier = Modifier.fillMaxSize(),
            folders = folders,
            contentPadding = PaddingValues(
                top = 22.dp,
                bottom = 44.dp,
            ),
            onAddFolderClick = onAddFolderClick,
            onRemoveClick = onRemoveClick,
        )
    }
}

@Composable
private fun ManageFoldersSheetContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    folders: List<LocalDirectory> = emptyList(),
    onAddFolderClick: () -> Unit = {},
    onRemoveClick: (LocalDirectory) -> Unit = {},
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(
                start = 22.dp,
                end = 22.dp,
                bottom = 16.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.manage_dirs),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.weight(1f))
            FilledTonalButton(
                onClick = onAddFolderClick,
                contentPadding = ButtonDefaults.TextButtonContentPadding,
            ) {
                Text(text = stringResource(R.string.add))
            }
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        LazyColumn(
            modifier = Modifier,
            contentPadding = contentPadding,
        ) {
            items(folders) {
                ListItem(
                    headlineContent = {
                        Text(text = it.path)
                    },
                    trailingContent = {
                        Icon(
                            modifier = Modifier.clickable {
                                onRemoveClick(it)
                            },
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.remove),
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
private fun PreviewManageFoldersSheetContent() {
    WallFlowTheme {
        Surface {
            ManageFoldersSheetContent(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 22.dp,
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
