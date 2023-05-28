package com.ammar.havenwalls.ui.settings

import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.havenwalls.DISABLED_ALPHA
import com.ammar.havenwalls.INTERNAL_MODELS
import com.ammar.havenwalls.R
import com.ammar.havenwalls.data.db.entity.ObjectDetectionModelEntity
import com.ammar.havenwalls.extensions.DELETE
import com.ammar.havenwalls.extensions.trimAll
import com.ammar.havenwalls.model.ObjectDetectionModel
import com.ammar.havenwalls.model.SavedSearch
import com.ammar.havenwalls.ui.common.ProgressIndicator
import com.ammar.havenwalls.ui.common.TextFieldState
import com.ammar.havenwalls.ui.theme.HavenWallsTheme
import com.ammar.havenwalls.utils.DownloadStatus
import org.tensorflow.lite.task.core.ComputeSettings.Delegate
import kotlin.math.roundToInt

@Composable
fun WallhavenApiKeyItem(
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(text = stringResource(R.string.wallhaven_api_key)) },
        supportingContent = {
            Column {
                Text(text = stringResource(R.string.wallhaven_api_key_desc))
                Text(
                    text = stringResource(R.string.wallhaven_api_key_desc_2),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
}

internal fun LazyListScope.accountSection(
    onWallhavenApiKeyItemClick: () -> Unit = {},
) {
    item { HeaderItem(stringResource(R.string.account)) }
    item {
        WallhavenApiKeyItem(
            modifier = Modifier.clickable(onClick = onWallhavenApiKeyItemClick)
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAccountSection() {
    HavenWallsTheme {
        Surface {
            LazyColumn {
                accountSection()
            }
        }
    }
}

internal fun LazyListScope.generalSection(
    blurSketchy: Boolean = false,
    blurNsfw: Boolean = false,
    onBlurSketchyCheckChange: (checked: Boolean) -> Unit = {},
    onBlurNsfwCheckChange: (checked: Boolean) -> Unit = {},
    onManageSavedSearchesClick: () -> Unit = {},
) {
    item { HeaderItem(stringResource(R.string.general)) }
    item {
        ListItem(
            modifier = Modifier.clickable { onBlurSketchyCheckChange(!blurSketchy) },
            headlineContent = { Text(text = "Blur sketchy wallpapers") },
            trailingContent = {
                Switch(
                    modifier = Modifier.height(24.dp),
                    checked = blurSketchy,
                    onCheckedChange = onBlurSketchyCheckChange,
                )
            },
        )
    }
    item {
        ListItem(
            modifier = Modifier.clickable { onBlurNsfwCheckChange(!blurNsfw) },
            headlineContent = { Text(text = "Blur NSFW wallpapers") },
            trailingContent = {
                Switch(
                    modifier = Modifier.height(24.dp),
                    checked = blurNsfw,
                    onCheckedChange = onBlurNsfwCheckChange,
                )
            },
        )
    }
    item {
        ListItem(
            modifier = Modifier.clickable(onClick = onManageSavedSearchesClick),
            headlineContent = { Text(text = stringResource(R.string.manager_saved_searches)) }
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewGeneralSection() {
    HavenWallsTheme {
        Surface {
            LazyColumn {
                generalSection()
            }
        }
    }
}

internal fun LazyListScope.objectDetectionSection(
    enabled: Boolean = false,
    delegate: Delegate = Delegate.GPU,
    model: ObjectDetectionModel = ObjectDetectionModel.DEFAULT,
    onEnabledChange: (enabled: Boolean) -> Unit = {},
    onDelegateClick: () -> Unit = {},
    onModelClick: () -> Unit = {},
) {
    val alpha = if (enabled) 1f else DISABLED_ALPHA

    item { HeaderItem(stringResource(R.string.object_detection)) }
    item {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.object_detection_setting_desc),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    item {
        ListItem(
            modifier = Modifier.clickable { onEnabledChange(!enabled) },
            headlineContent = { Text(text = "Enable object detection") },
            trailingContent = {
                Switch(
                    modifier = Modifier.height(24.dp),
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                )
            },
        )
    }
    item {
        ListItem(
            modifier = Modifier.clickable(
                enabled = enabled,
                onClick = onDelegateClick,
            ),
            headlineContent = {
                Text(
                    text = "TFLite delegate",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
            },
            supportingContent = {
                Text(
                    text = delegateString(delegate),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
            },
        )
    }
    item {
        ListItem(
            modifier = Modifier.clickable(
                enabled = enabled,
                onClick = onModelClick,
            ),
            headlineContent = {
                Text(
                    text = "TFLite model",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
            },
            supportingContent = {
                Text(
                    text = model.name,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
            },
        )
    }
}

@Composable
private fun delegateString(delegate: Delegate) = stringResource(
    when (delegate) {
        Delegate.NONE -> R.string.cpu
        Delegate.NNAPI -> R.string.nnapi
        Delegate.GPU -> R.string.gpu
    }
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSubjectDetectionSection() {
    HavenWallsTheme {
        Surface {
            LazyColumn {
                objectDetectionSection()
            }
        }
    }
}

@Composable
internal fun HeaderItem(text: String) {
    Text(
        modifier = Modifier.padding(
            horizontal = 16.dp,
            vertical = 8.dp,
        ),
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

internal fun LazyListScope.dividerItem() {
    item { Divider(modifier = Modifier.fillMaxWidth()) }
}

@Composable
fun ObjectDetectionDelegateOptionsDialog(
    modifier: Modifier = Modifier,
    selectedDelegate: Delegate? = null,
    onSaveClick: (delegate: Delegate) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    var localSelectedDelegate by remember(selectedDelegate) {
        mutableStateOf(selectedDelegate ?: Delegate.NONE)
    }
    AlertDialog(
        modifier = modifier,
        title = { Text(text = "Choose delegate for TFLite") },
        text = {
            ObjectDetectionDelegateOptionsContent(
                selectedDelegate = localSelectedDelegate,
                onOptionClick = { localSelectedDelegate = it },
            )
        },
        confirmButton = {
            TextButton(onClick = { onSaveClick(localSelectedDelegate) }) {
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

@Composable
fun ObjectDetectionDelegateOptionsContent(
    modifier: Modifier = Modifier,
    selectedDelegate: Delegate = Delegate.NONE,
    onOptionClick: (delegate: Delegate) -> Unit = {},
) {
    Column(modifier = modifier) {
        Delegate.values()
            .filter {
                if (it != Delegate.NNAPI) {
                    true
                } else {
                    // NNAPI is only supported for Android Pie or above
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                }
            }
            .map {
                ListItem(
                    modifier = Modifier.clickable(onClick = { onOptionClick(it) }),
                    headlineContent = { Text(text = delegateString(it)) },
                    leadingContent = {
                        RadioButton(
                            modifier = Modifier.size(24.dp),
                            selected = selectedDelegate == it,
                            onClick = { onOptionClick(it) },
                        )
                    },
                )
            }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewObjectDetectionDelegateOptionsDialog() {
    HavenWallsTheme {
        Surface {
            ObjectDetectionDelegateOptionsDialog()
        }
    }
}

@Composable
fun ObjectDetectionModelOptionsDialog(
    modifier: Modifier = Modifier,
    models: List<ObjectDetectionModelEntity> = emptyList(),
    selectedModelId: Long? = null,
    onOptionEditClick: (model: ObjectDetectionModelEntity) -> Unit = {},
    onAddClick: () -> Unit = {},
    onSaveClick: (entity: ObjectDetectionModelEntity?) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    var localSelectedModel by remember(selectedModelId) {
        mutableStateOf(models.find { it.id == selectedModelId } ?: models.firstOrNull())
    }
    key(models.size) {
        AlertDialog(
            modifier = modifier,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.tensorflow),
                    contentDescription = ""
                )
            },
            title = { Text(text = "Choose a TFLite model") },
            text = {
                ObjectDetectionModelOptionsContent(
                    models = models,
                    selectedModel = localSelectedModel,
                    onOptionClick = { localSelectedModel = it },
                    onOptionEditClick = onOptionEditClick,
                    onAddClick = onAddClick,
                )
            },
            confirmButton = {
                TextButton(onClick = { onSaveClick(localSelectedModel) }) {
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
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewObjectDetectionModelOptionsDialog() {
    HavenWallsTheme {
        Surface {
            ObjectDetectionModelOptionsDialog(
                models = List(3) {
                    ObjectDetectionModelEntity(
                        id = it.toLong(),
                        name = "model_$it",
                        fileName = "file_name_$it",
                        url = "url_$it",
                    )
                }
            )
        }
    }
}

@Composable
fun ObjectDetectionModelOptionsContent(
    modifier: Modifier = Modifier,
    models: List<ObjectDetectionModelEntity> = emptyList(),
    selectedModel: ObjectDetectionModelEntity? = null,
    onOptionClick: (model: ObjectDetectionModelEntity) -> Unit = {},
    onOptionEditClick: (model: ObjectDetectionModelEntity) -> Unit = {},
    onAddClick: () -> Unit = {},
) {
    Column(modifier = modifier) {
        models.map {
            ListItem(
                modifier = Modifier.clickable(onClick = { onOptionClick(it) }),
                headlineContent = { Text(text = it.name) },
                leadingContent = {
                    RadioButton(
                        modifier = Modifier.size(24.dp),
                        selected = selectedModel?.id == it.id,
                        onClick = { onOptionClick(it) },
                    )
                },
                trailingContent = if (it.name in INTERNAL_MODELS) null else {
                    {
                        IconButton(onClick = { onOptionEditClick(it) }) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                            )
                        }
                    }
                }
            )
        }
        ListItem(
            modifier = Modifier.clickable(onClick = onAddClick),
            colors = ListItemDefaults.colors(
                leadingIconColor = MaterialTheme.colorScheme.primary,
                headlineColor = MaterialTheme.colorScheme.primary,
            ),
            headlineContent = { Text(text = stringResource(R.string.add)) },
            leadingContent = {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add"
                )
            },
        )
    }
}

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
    val nameState by rememberSaveable(stateSaver = NameStateSaver) {
        mutableStateOf(NameState(model?.name ?: "", nameExists))
    }
    val urlState by rememberSaveable(stateSaver = UrlStateSaver) {
        mutableStateOf(UrlState(model?.url ?: ""))
    }

    LaunchedEffect(nameState.text) {
        nameExists.value = checkNameExists(nameState.text.trimAll(), model?.id)
    }

    key(saving) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismissRequest,
        ) {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = if (model == null) "Add a TFLite model" else "Edit model",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(modifier = Modifier.requiredHeight(16.dp))
                    ObjectDetectionModelEditContent(
                        nameState = nameState,
                        urlState = urlState,
                        saving = saving,
                        downloadStatus = downloadStatus,
                    )
                    Spacer(modifier = Modifier.requiredHeight(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
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
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            enabled = !saving,
                            onClick = onDismissRequest,
                        ) {
                            Text(text = stringResource(R.string.cancel))
                        }
                        Spacer(modifier = Modifier.requiredWidth(8.dp))
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
                                    )
                                ) { saving = false }
                            },
                        ) {
                            Text(text = stringResource(if (model == null) R.string.add else R.string.save))
                        }
                    }
                }

            }
        }
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
        )
    )

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewObjectDetectionModelEditDialog(
    @PreviewParameter(ModelParameterProvider::class) model: ObjectDetectionModelEntity?,
) {
    HavenWallsTheme {
        Surface {
            ObjectDetectionModelEditDialog(
                model = model,
                downloadStatus = DownloadStatus.Running(
                    downloadedBytes = 500,
                    totalBytes = 1000,
                )
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
            label = { Text(text = "Name") },
            isError = nameState.showErrors(),
            supportingText = { Text(text = nameState.getError() ?: "") }
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
            supportingText = { Text(text = urlState.getError() ?: "") }
        )
        if (downloadStatus != null) {
            val showProgress = remember(downloadStatus) {
                (downloadStatus is DownloadStatus.Running
                        || downloadStatus is DownloadStatus.Paused
                        || downloadStatus is DownloadStatus.Pending)
            }
            val progress by animateFloatAsState(
                when (downloadStatus) {
                    is DownloadStatus.Running -> downloadStatus.progress
                    is DownloadStatus.Paused -> downloadStatus.progress
                    else -> 0F
                }
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

@Composable
fun ObjectDetectionModelDeleteConfirmDialog(
    modifier: Modifier = Modifier,
    model: ObjectDetectionModelEntity,
    onConfirmClick: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    AlertDialog(
        modifier = modifier,
        title = { Text(text = "Delete model '${model.name}'?") },
        text = { Text(text = "Downloaded model will by permanently removed from local storage.") },
        confirmButton = {
            TextButton(onClick = onConfirmClick) {
                Text(text = stringResource(R.string.delete))
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
private fun PreviewOObjectDetectionModelDeleteConfirmDialog() {
    HavenWallsTheme {
        Surface {
            ObjectDetectionModelDeleteConfirmDialog(
                model = ObjectDetectionModelEntity(
                    id = 1,
                    name = "model_1",
                    fileName = "file_name_1",
                    url = "url_1",
                )
            )
        }
    }
}

@Composable
fun EditSavedSearchBottomSheetHeader(
    name: String = "",
    saveEnabled: Boolean = true,
    onSaveClick: () -> Unit = {},
    onNameChange: (String) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 22.dp,
                end = 22.dp,
                bottom = 16.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.edit_saved_search),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.requiredWidth(8.dp))
        Button(
            enabled = saveEnabled,
            onClick = onSaveClick,
        ) {
            Text(stringResource(R.string.save))
        }
    }
    Divider(modifier = Modifier.fillMaxWidth())
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 16.dp,
                start = 22.dp,
                end = 22.dp,
            ),
        label = { Text(text = stringResource(R.string.name)) },
        value = name,
        singleLine = true,
        onValueChange = onNameChange,
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewEditSearchBottomSheetHeader() {
    HavenWallsTheme {
        Surface {
            Column {
                EditSavedSearchBottomSheetHeader()
            }
        }
    }
}

@Composable
fun DeleteSavedSearchConfirmDialog(
    modifier: Modifier = Modifier,
    savedSearch: SavedSearch,
    onConfirmClick: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    AlertDialog(
        modifier = modifier,
        title = { Text(text = "Delete saved search '${savedSearch.name}'?") },
        confirmButton = {
            TextButton(onClick = onConfirmClick) {
                Text(text = stringResource(R.string.delete))
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
private fun PreviewDeleteSavedSearchConfirmDialog() {
    HavenWallsTheme {
        Surface {
            DeleteSavedSearchConfirmDialog(
                savedSearch = SavedSearch(
                    name = "test",
                )
            )
        }
    }
}
