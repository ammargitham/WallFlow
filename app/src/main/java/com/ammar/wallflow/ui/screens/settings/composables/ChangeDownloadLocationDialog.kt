package com.ammar.wallflow.ui.screens.settings.composables

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.common.UnpaddedAlertDialogContent
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.utils.getRealPath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeDownloadLocationDialog(
    modifier: Modifier = Modifier,
    defaultLocation: Uri = Uri.EMPTY,
    customLocation: Uri? = null,
    onDefaultClick: () -> Unit = {},
    onCustomClick: () -> Unit = {},
    onCustomEditClick: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        UnpaddedAlertDialogContent(
            title = { Text(text = stringResource(R.string.download_location)) },
            text = {
                ChangeDownloadLocationDialogContent(
                    defaultLocation = defaultLocation,
                    customLocation = customLocation,
                    onDefaultClick = onDefaultClick,
                    onCustomClick = onCustomClick,
                    onCustomEditClick = onCustomEditClick,
                )
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }
            },
        )
    }
}

@Composable
private fun ChangeDownloadLocationDialogContent(
    modifier: Modifier = Modifier,
    defaultLocation: Uri = Uri.EMPTY,
    customLocation: Uri? = null,
    onDefaultClick: () -> Unit = {},
    onCustomClick: () -> Unit = {},
    onCustomEditClick: () -> Unit = {},
) {
    val context = LocalContext.current

    Column(modifier = modifier) {
        ListItem(
            modifier = Modifier
                .clickable(onClick = onDefaultClick)
                .padding(horizontal = 8.dp),
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            headlineContent = {
                Text(
                    text = getRealPath(context, defaultLocation)
                        ?: defaultLocation.path
                        ?: "",
                )
            },
            leadingContent = {
                RadioButton(
                    modifier = Modifier.size(24.dp),
                    selected = customLocation == null,
                    onClick = onDefaultClick,
                )
            },
        )
        ListItem(
            modifier = Modifier
                .clickable(onClick = onCustomClick)
                .padding(horizontal = 8.dp),
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            headlineContent = {
                Text(
                    text = if (customLocation == null) {
                        stringResource(R.string.custom)
                    } else {
                        getRealPath(context, customLocation)
                            ?: customLocation.path
                            ?: ""
                    },
                )
            },
            leadingContent = {
                RadioButton(
                    modifier = Modifier.size(24.dp),
                    selected = customLocation != null,
                    onClick = onCustomClick,
                )
            },
            trailingContent = if (customLocation != null) {
                {
                    IconButton(onClick = onCustomEditClick) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                        )
                    }
                }
            } else {
                null
            },
        )
    }
}

private data class ChangeDownloadLocationDialogParams(
    val defaultLocation: Uri = Uri.EMPTY,
    val customLocation: Uri? = null,
)

private class ChangeDownloadLocationDialogPPP :
    CollectionPreviewParameterProvider<ChangeDownloadLocationDialogParams>(
        listOf(
            ChangeDownloadLocationDialogParams(),
            ChangeDownloadLocationDialogParams(
                defaultLocation = Uri.EMPTY,
            ),
        ),
    )

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewChangeDownloadLocationDialog(
    @PreviewParameter(
        ChangeDownloadLocationDialogPPP::class,
    ) params: ChangeDownloadLocationDialogParams,
) {
    WallFlowTheme {
        Surface {
            ChangeDownloadLocationDialog(
                defaultLocation = params.defaultLocation,
                customLocation = params.customLocation,
            )
        }
    }
}
