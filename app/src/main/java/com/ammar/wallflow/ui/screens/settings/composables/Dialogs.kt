package com.ammar.wallflow.ui.screens.settings.composables

import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.Constraints
import com.ammar.wallflow.DISABLED_ALPHA
import com.ammar.wallflow.R
import com.ammar.wallflow.data.db.entity.ObjectDetectionModelEntity
import com.ammar.wallflow.data.preferences.ObjectDetectionDelegate
import com.ammar.wallflow.data.preferences.Theme
import com.ammar.wallflow.data.preferences.defaultAutoWallpaperConstraints
import com.ammar.wallflow.data.preferences.defaultAutoWallpaperFreq
import com.ammar.wallflow.extensions.toConstraintTypeMap
import com.ammar.wallflow.extensions.toConstraints
import com.ammar.wallflow.model.ConstraintType
import com.ammar.wallflow.model.search.SavedSearch
import com.ammar.wallflow.ui.common.UnpaddedAlertDialogContent
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.utils.ExifWriteType
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimePeriod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectDetectionDelegateOptionsDialog(
    modifier: Modifier = Modifier,
    selectedDelegate: ObjectDetectionDelegate? = null,
    onSaveClick: (delegate: ObjectDetectionDelegate) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    var localSelectedDelegate by remember(selectedDelegate) {
        mutableStateOf(selectedDelegate ?: ObjectDetectionDelegate.NONE)
    }
    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        UnpaddedAlertDialogContent(
            title = { Text(text = stringResource(R.string.choose_delegate_for_tflite)) },
            text = {
                ObjectDetectionDelegateOptionsContent(
                    selectedDelegate = localSelectedDelegate,
                    onOptionClick = { localSelectedDelegate = it },
                )
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(onClick = { onSaveClick(localSelectedDelegate) }) {
                        Text(text = stringResource(R.string.save))
                    }
                }
            },
        )
    }
}

@Composable
private fun ObjectDetectionDelegateOptionsContent(
    modifier: Modifier = Modifier,
    selectedDelegate: ObjectDetectionDelegate = ObjectDetectionDelegate.NONE,
    onOptionClick: (delegate: ObjectDetectionDelegate) -> Unit = {},
) {
    Column(modifier = modifier) {
        ObjectDetectionDelegate.entries
            .filter {
                if (it != ObjectDetectionDelegate.NNAPI) {
                    true
                } else {
                    // NNAPI is only supported for Android Pie or above
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                }
            }
            .map {
                ListItem(
                    modifier = Modifier
                        .clickable(onClick = { onOptionClick(it) })
                        .padding(horizontal = 8.dp),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
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
    WallFlowTheme {
        Surface {
            ObjectDetectionDelegateOptionsDialog()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        UnpaddedAlertDialogContent(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.tensorflow),
                    contentDescription = "",
                )
            },
            title = { Text(text = stringResource(R.string.choose_a_tflite_model)) },
            text = {
                ObjectDetectionModelOptionsContent(
                    models = models,
                    selectedModel = localSelectedModel,
                    onOptionClick = { localSelectedModel = it },
                    onOptionEditClick = onOptionEditClick,
                    onAddClick = onAddClick,
                )
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(onClick = { onSaveClick(localSelectedModel) }) {
                        Text(text = stringResource(R.string.save))
                    }
                }
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewObjectDetectionModelOptionsDialog() {
    var models by remember {
        mutableStateOf(
            List(3) {
                ObjectDetectionModelEntity(
                    id = it.toLong(),
                    name = "model_$it",
                    fileName = "file_name_$it",
                    url = "url_$it",
                )
            },
        )
    }
    WallFlowTheme {
        Surface {
            ObjectDetectionModelOptionsDialog(
                models = models,
                onAddClick = {
                    val size = models.size
                    models = models + ObjectDetectionModelEntity(
                        id = size.toLong(),
                        name = "model_$size",
                        fileName = "file_name_$size",
                        url = "url_$size",
                    )
                },
            )
        }
    }
}

@Composable
private fun ObjectDetectionModelOptionsContent(
    modifier: Modifier = Modifier,
    models: List<ObjectDetectionModelEntity> = emptyList(),
    selectedModel: ObjectDetectionModelEntity? = null,
    onOptionClick: (model: ObjectDetectionModelEntity) -> Unit = {},
    onOptionEditClick: (model: ObjectDetectionModelEntity) -> Unit = {},
    onAddClick: () -> Unit = {},
) {
    Column(
        modifier = modifier.scrollable(
            state = rememberScrollState(),
            orientation = Orientation.Vertical,
        ),
    ) {
        models.map {
            ListItem(
                modifier = Modifier
                    .clickable(onClick = { onOptionClick(it) })
                    .padding(horizontal = 8.dp),
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                headlineContent = { Text(text = it.name) },
                leadingContent = {
                    RadioButton(
                        modifier = Modifier.size(24.dp),
                        selected = selectedModel?.id == it.id,
                        onClick = { onOptionClick(it) },
                    )
                },
                trailingContent = {
                    IconButton(onClick = { onOptionEditClick(it) }) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                        )
                    }
                },
            )
        }
        ListItem(
            modifier = Modifier
                .clickable(onClick = onAddClick)
                .padding(horizontal = 8.dp),
            colors = ListItemDefaults.colors(
                leadingIconColor = MaterialTheme.colorScheme.primary,
                headlineColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            headlineContent = { Text(text = stringResource(R.string.add)) },
            leadingContent = {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add),
                )
            },
        )
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
        title = { Text(text = stringResource(R.string.delete_model_dialog_title, model.name)) },
        text = { Text(text = stringResource(R.string.delete_model_dialog_text)) },
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
    WallFlowTheme {
        Surface {
            ObjectDetectionModelDeleteConfirmDialog(
                model = ObjectDetectionModelEntity(
                    id = 1,
                    name = "model_1",
                    fileName = "file_name_1",
                    url = "url_1",
                ),
            )
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
        title = {
            Text(
                text = stringResource(
                    R.string.delete_saved_search_dialog_title,
                    savedSearch.name,
                ),
            )
        },
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
private fun PreviewDeleteSavedSearchConfirmDialog() {
    WallFlowTheme {
        Surface {
            DeleteSavedSearchConfirmDialog(
                savedSearch = SavedSearch(
                    name = "test",
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencyDialog(
    modifier: Modifier = Modifier,
    frequency: DateTimePeriod = defaultAutoWallpaperFreq,
    onSaveClick: (DateTimePeriod) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    var value by rememberSaveable(
        frequency,
        stateSaver = TextFieldValue.Saver,
    ) {
        val hours = frequency.hours
        val minutes = frequency.minutes
        val value = if (hours != 0 && minutes == 0) {
            frequency.hours
        } else {
            frequency.hours * 60 + frequency.minutes
        }
        mutableStateOf(TextFieldValue(value.toString()))
    }
    var selectedChronoUnit by rememberSaveable(frequency) {
        val hours = frequency.hours
        val minutes = frequency.minutes
        mutableStateOf(
            if (hours != 0 && minutes == 0) {
                FrequencyChronoUnit.HOURS
            } else {
                FrequencyChronoUnit.MINUTES
            },
        )
    }
    var expanded by rememberSaveable { mutableStateOf(false) }

    fun getFrequency(): DateTimePeriod {
        val inputInt = value.text.toIntOrNull() ?: 0
        return when (selectedChronoUnit) {
            FrequencyChronoUnit.HOURS -> DateTimePeriod(hours = inputInt)
            FrequencyChronoUnit.MINUTES -> DateTimePeriod(minutes = inputInt)
        }
    }

    val hasMinError by rememberSaveable(value, selectedChronoUnit) {
        val freq = getFrequency()
        val minutes = freq.hours * 60 + freq.minutes
        mutableStateOf(minutes < 15)
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // put cursor at end of value
        value = value.copy(selection = TextRange(value.text.length))
        delay(200)
        focusRequester.requestFocus()
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.frequency)) },
        text = {
            OutlinedTextField(
                modifier = Modifier.focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val newFreq = getFrequency()
                        if (newFreq.hours <= 0 && newFreq.minutes <= 0) {
                            return@KeyboardActions
                        }
                        onSaveClick(newFreq)
                    },
                ),
                value = value,
                isError = hasMinError,
                supportingText = if (hasMinError) {
                    {
                        Text(text = stringResource(R.string.frequency_min_val_error))
                    }
                } else {
                    null
                },
                onValueChange = { newValue ->
                    val newText = newValue.text
                    if (newText.isEmpty()) {
                        value = newValue
                        return@OutlinedTextField
                    }
                    val newTextInt = newText.toIntOrNull() ?: 0
                    if (newTextInt >= 0) {
                        value = newValue
                    }
                },
                suffix = {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                    ) {
                        Row {
                            Text(
                                modifier = Modifier.menuAnchor(),
                                text = chronoUnitString(selectedChronoUnit),
                            )
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                        ExposedDropdownMenu(
                            modifier = Modifier.width(IntrinsicSize.Min),
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            FrequencyChronoUnit.entries.forEach { chronoUnit ->
                                DropdownMenuItem(
                                    text = { Text(text = chronoUnitString(chronoUnit)) },
                                    onClick = {
                                        selectedChronoUnit = chronoUnit
                                        expanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }
                },
            )
        },
        confirmButton = {
            TextButton(
                enabled = (value.text.toIntOrNull() ?: 0) > 0,
                onClick = { onSaveClick(getFrequency()) },
            ) {
                Text(text = stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFrequencyDialog() {
    WallFlowTheme {
        Surface {
            FrequencyDialog(
                frequency = DateTimePeriod.parse("PT60M"),
                onSaveClick = {
                    Log.d("PreviewFrequencyDialog", "newFreq: $it")
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConstraintOptionsDialog(
    modifier: Modifier = Modifier,
    constraints: Constraints = defaultAutoWallpaperConstraints,
    onSaveClick: (Constraints) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    var localConstraints by remember(constraints) { mutableStateOf(constraints) }

    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        UnpaddedAlertDialogContent(
            title = { Text(text = stringResource(R.string.constraints)) },
            text = {
                ConstraintOptionsDialogContent(
                    constraintTypeMap = localConstraints.toConstraintTypeMap(),
                    onChange = { localConstraints = it.toConstraints() },
                )
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(onClick = { onSaveClick(localConstraints) }) {
                        Text(text = stringResource(R.string.save))
                    }
                }
            },
        )
    }
}

@Composable
private fun ConstraintOptionsDialogContent(
    modifier: Modifier = Modifier,
    constraintTypeMap: Map<ConstraintType, Boolean> = defaultAutoWallpaperConstraints
        .toConstraintTypeMap(),
    onChange: (Map<ConstraintType, Boolean>) -> Unit = {},
) {
    val wifiChecked = constraintTypeMap[ConstraintType.WIFI] ?: false
    val roamingEnabled = !wifiChecked

    Column(
        modifier = modifier.scrollable(
            state = rememberScrollState(),
            orientation = Orientation.Vertical,
        ),
    ) {
        ConstraintOption(
            label = stringResource(R.string.on_wifi),
            desc = stringResource(R.string.on_wifi_desc),
            checked = wifiChecked,
            enabled = true,
            onCheckedChange = {
                onChange(constraintTypeMap + (ConstraintType.WIFI to it))
            },
        )
        ConstraintOption(
            label = stringResource(R.string.data_roaming),
            desc = stringResource(R.string.data_roaming_desc),
            checked = constraintTypeMap[ConstraintType.ROAMING] ?: false,
            enabled = roamingEnabled,
            onCheckedChange = {
                onChange(constraintTypeMap + (ConstraintType.ROAMING to it))
            },
        )
        ConstraintOption(
            label = stringResource(R.string.charging),
            desc = stringResource(R.string.charging_desc),
            checked = constraintTypeMap[ConstraintType.CHARGING] ?: false,
            enabled = true,
            onCheckedChange = {
                onChange(constraintTypeMap + (ConstraintType.CHARGING to it))
            },
        )
        ConstraintOption(
            label = stringResource(R.string.idle),
            desc = stringResource(R.string.idle_desc),
            checked = constraintTypeMap[ConstraintType.IDLE] ?: false,
            enabled = true,
            onCheckedChange = {
                onChange(constraintTypeMap + (ConstraintType.IDLE to it))
            },
        )
    }
}

@Composable
private fun ConstraintOption(
    label: String,
    desc: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        modifier = Modifier
            .clickable(
                enabled = enabled,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(horizontal = 8.dp),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        headlineContent = {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (enabled) 1f else DISABLED_ALPHA,
                ),
            )
        },
        supportingContent = {
            Text(
                text = desc,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (enabled) 1f else DISABLED_ALPHA,
                ),
            )
        },
        leadingContent = {
            Checkbox(
                modifier = Modifier.size(24.dp),
                enabled = enabled,
                checked = checked,
                onCheckedChange = { onCheckedChange(it) },
            )
        },
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewConstraintOptionsDialog() {
    WallFlowTheme {
        Surface {
            ConstraintOptionsDialog()
        }
    }
}

@Composable
fun NextRunInfoDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
) {
    AlertDialog(
        modifier = modifier,
        text = { Text(text = stringResource(R.string.next_run_time_info)) },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.ok))
            }
        },
        onDismissRequest = onDismissRequest,
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewNextRunInfoDialog() {
    WallFlowTheme {
        Surface {
            NextRunInfoDialog()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeOptionsDialog(
    modifier: Modifier = Modifier,
    theme: Theme = Theme.SYSTEM,
    onSaveClick: (Theme) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    var localSelectedTheme by remember(theme) { mutableStateOf(theme) }

    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        UnpaddedAlertDialogContent(
            title = { Text(text = stringResource(R.string.theme)) },
            text = {
                ThemeOptionsContent(
                    selectedTheme = localSelectedTheme,
                    onOptionClick = { localSelectedTheme = it },
                )
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(onClick = { onSaveClick(localSelectedTheme) }) {
                        Text(text = stringResource(R.string.save))
                    }
                }
            },
        )
    }
}

@Composable
private fun ThemeOptionsContent(
    modifier: Modifier = Modifier,
    selectedTheme: Theme = Theme.SYSTEM,
    onOptionClick: (Theme) -> Unit = {},
) {
    Column(modifier = modifier) {
        Theme.entries.map {
            ListItem(
                modifier = Modifier
                    .clickable(onClick = { onOptionClick(it) })
                    .padding(horizontal = 8.dp),
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                headlineContent = { Text(text = themeString(it)) },
                leadingContent = {
                    RadioButton(
                        modifier = Modifier.size(24.dp),
                        selected = selectedTheme == it,
                        onClick = { onOptionClick(it) },
                    )
                },
            )
        }
    }
}

@Composable
private fun themeString(theme: Theme) = stringResource(
    when (theme) {
        Theme.SYSTEM -> R.string.system
        Theme.LIGHT -> R.string.light
        Theme.DARK -> R.string.dark
    },
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewThemeOptionsDialog() {
    WallFlowTheme {
        Surface {
            ThemeOptionsDialog()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExifWriteTypeOptionsDialog(
    modifier: Modifier = Modifier,
    selectedExifWriteType: ExifWriteType = ExifWriteType.APPEND,
    onSaveClick: (exifWriteType: ExifWriteType) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    var localSelectedWriteType by remember(selectedExifWriteType) {
        mutableStateOf(selectedExifWriteType)
    }
    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        UnpaddedAlertDialogContent(
            title = { Text(text = stringResource(R.string.exif_write_type)) },
            text = {
                ExifWriteTypeOptionsContent(
                    selectedExifWriteType = localSelectedWriteType,
                    onOptionClick = { localSelectedWriteType = it },
                )
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(onClick = { onSaveClick(localSelectedWriteType) }) {
                        Text(text = stringResource(R.string.save))
                    }
                }
            },
        )
    }
}

@Composable
private fun ExifWriteTypeOptionsContent(
    modifier: Modifier = Modifier,
    selectedExifWriteType: ExifWriteType = ExifWriteType.APPEND,
    onOptionClick: (exifWriteType: ExifWriteType) -> Unit = {},
) {
    Column(modifier = modifier) {
        ExifWriteType.entries
            .map {
                ListItem(
                    modifier = Modifier
                        .clickable(onClick = { onOptionClick(it) })
                        .padding(horizontal = 8.dp),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    headlineContent = { Text(text = exifWriteTypeString(it)) },
                    leadingContent = {
                        RadioButton(
                            modifier = Modifier.size(24.dp),
                            selected = selectedExifWriteType == it,
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
private fun PreviewExifWriteTypeOptionsDialog() {
    WallFlowTheme {
        Surface {
            ExifWriteTypeOptionsDialog()
        }
    }
}

@Composable
fun ClearViewedWallpapersConfirmDialog(
    modifier: Modifier = Modifier,
    onConfirmClick: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    AlertDialog(
        modifier = modifier,
        title = {
            Text(text = stringResource(R.string.clear_viewed_wallpaper_history))
        },
        confirmButton = {
            TextButton(onClick = onConfirmClick) {
                Text(text = stringResource(R.string.clear))
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
private fun PreviewClearViewedWallpapersConfirmDialog() {
    WallFlowTheme {
        Surface {
            ClearViewedWallpapersConfirmDialog()
        }
    }
}
