package com.ammar.wallflow.ui.screens.settings.composables

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.data.db.entity.ObjectDetectionModelEntity
import com.ammar.wallflow.extensions.DELETE
import com.ammar.wallflow.extensions.trimAll
import com.ammar.wallflow.ui.common.NameState
import com.ammar.wallflow.ui.common.ProgressIndicator
import com.ammar.wallflow.ui.common.TextFieldState
import com.ammar.wallflow.ui.common.UnpaddedAlertDialogContent
import com.ammar.wallflow.ui.common.UrlState
import com.ammar.wallflow.ui.common.nameStateSaver
import com.ammar.wallflow.ui.common.urlStateSaver
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.utils.DownloadStatus
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectDetectionModelEditDialog(
    modifier: Modifier = Modifier,
    model: ObjectDetectionModelEntity? = null,
    downloadStatus: DownloadStatus? = null,
    checkNameExists: suspend (name: String, id: Long?) -> Boolean = { _, _ -> true },
    onSaveClick: (
        entity: ObjectDetectionModelEntity,
        onDone: (error: Throwable?) -> Unit,
    ) -> Unit = { _, _ -> },
    onDeleteClick: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val nameExists = remember { mutableStateOf(false) } // do not change to `by`
    var saving by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val nameState by rememberSaveable(stateSaver = nameStateSaver(context)) {
        mutableStateOf(NameState(context, model?.name ?: "", nameExists))
    }
    val urlState by rememberSaveable(stateSaver = urlStateSaver(context)) {
        mutableStateOf(UrlState(context, model?.url ?: ""))
    }

    LaunchedEffect(nameState.text) {
        nameExists.value = checkNameExists(nameState.text.trimAll(), model?.id)
    }

    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        UnpaddedAlertDialogContent(
            title = {
                Text(
                    text = stringResource(
                        if (model == null) R.string.add_a_tflite_model else R.string.edit_model,
                    ),
                )
            },
            text = {
                ObjectDetectionModelEditContent(
                    modifier = modifier.padding(24.dp),
                    nameState = nameState,
                    urlState = urlState,
                    saving = saving,
                    downloadStatus = downloadStatus,
                )
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (model != null) {
                        FilledTonalButton(
                            enabled = !saving,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.DELETE,
                                contentColor = Color.White,
                            ),
                            onClick = onDeleteClick,
                        ) {
                            Text(text = stringResource(R.string.delete))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    TextButton(
                        enabled = !saving,
                        onClick = onDismissRequest,
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(
                        enabled = !saving && nameState.isValid && urlState.isValid,
                        onClick = {
                            saving = true
                            onSaveClick(
                                model?.copy(
                                    name = nameState.text.trimAll(),
                                    url = urlState.text.trimAll(),
                                ) ?: ObjectDetectionModelEntity(
                                    id = 0L,
                                    name = nameState.text.trimAll(),
                                    url = urlState.text.trimAll(),
                                    fileName = "",
                                ),
                            ) { saving = false }
                        },
                    ) {
                        Text(
                            text = stringResource(
                                if (model == null) R.string.add else R.string.save,
                            ),
                        )
                    }
                }
            },
        )
    }
}

private class ModelParameterProvider :
    CollectionPreviewParameterProvider<ObjectDetectionModelEntity?>(
        listOf(
            null,
            ObjectDetectionModelEntity(
                id = 1L,
                name = "model name",
                fileName = "file name",
                url = "http://example.com",
            ),
        ),
    )

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewObjectDetectionModelEditDialog(
    @PreviewParameter(ModelParameterProvider::class) model: ObjectDetectionModelEntity?,
) {
    val coroutineScope = rememberCoroutineScope()
    var downloadStatus: DownloadStatus? by remember { mutableStateOf(null) }
    WallFlowTheme {
        Surface {
            ObjectDetectionModelEditDialog(
                model = model,
                downloadStatus = downloadStatus,
                checkNameExists = { _, _ -> false },
                onSaveClick = { _, onDone ->
                    coroutineScope.launch {
                        downloadStatus = DownloadStatus.Running(
                            downloadedBytes = 500,
                            totalBytes = 1000,
                        )
                        delay(5000)
                        downloadStatus = null
                        onDone(null)
                    }
                },
            )
        }
    }
}

@Composable
fun ObjectDetectionModelEditContent(
    modifier: Modifier = Modifier,
    nameState: TextFieldState = TextFieldState(),
    urlState: TextFieldState = TextFieldState(),
    saving: Boolean = false,
    downloadStatus: DownloadStatus? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    nameState.onFocusChange(focusState.isFocused)
                    if (!focusState.isFocused) {
                        nameState.enableShowErrors()
                    }
                },
            enabled = !saving,
            value = nameState.text,
            onValueChange = {
                nameState.text = it
                nameState.enableShowErrors()
            },
            singleLine = true,
            label = { Text(text = stringResource(R.string.name)) },
            isError = nameState.showErrors(),
            supportingText = { Text(text = nameState.getError() ?: "") },
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    urlState.onFocusChange(focusState.isFocused)
                    if (!focusState.isFocused) {
                        urlState.enableShowErrors()
                    }
                },
            enabled = !saving,
            value = urlState.text,
            onValueChange = { urlState.text = it },
            singleLine = true,
            label = { Text(text = "URL") },
            isError = urlState.showErrors(),
            supportingText = { Text(text = urlState.getError() ?: "") },
        )
        if (downloadStatus != null) {
            val showProgress = remember(downloadStatus) {
                (
                    downloadStatus is DownloadStatus.Running ||
                        downloadStatus is DownloadStatus.Paused ||
                        downloadStatus is DownloadStatus.Pending
                    )
            }
            val progress by animateFloatAsState(
                when (downloadStatus) {
                    is DownloadStatus.Running -> downloadStatus.progress
                    is DownloadStatus.Paused -> downloadStatus.progress
                    else -> 0F
                },
                label = "progress",
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showProgress) {
                    ProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        progress = progress,
                    )
                }
                val text = when (downloadStatus) {
                    is DownloadStatus.Cancelled -> R.string.cancelled
                    is DownloadStatus.Failed -> R.string.failed
                    is DownloadStatus.Paused -> R.string.paused
                    is DownloadStatus.Pending -> R.string.pending
                    is DownloadStatus.Running -> R.string.downloading
                    is DownloadStatus.Success -> R.string.downloaded
                }
                Text(text = stringResource(text))
                if (showProgress) {
                    Text(text = "(${(progress * 100).roundToInt()}%)")
                }
            }
        }
    }
}
