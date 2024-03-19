package com.ammar.wallflow.ui.screens.backuprestore

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.DISABLED_ALPHA
import com.ammar.wallflow.R
import com.ammar.wallflow.model.backup.BackupOptions
import com.ammar.wallflow.model.backup.FileNotFoundException
import com.ammar.wallflow.model.backup.InvalidJsonException
import com.ammar.wallflow.model.backup.RestoreException
import com.ammar.wallflow.model.backup.RestoreSummary
import com.ammar.wallflow.ui.common.ProgressIndicator
import com.ammar.wallflow.ui.common.UnpaddedAlertDialogContent
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.utils.getAlpha
import com.ammar.wallflow.utils.getRealPath
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupDialog(
    modifier: Modifier = Modifier,
    options: BackupOptions = BackupOptions(),
    backupProgress: Float? = null,
    onFileInputClicked: () -> Unit = {},
    onOptionsChange: (BackupOptions) -> Unit = {},
    onBackupClick: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val backupBtnEnabled by remember(backupProgress, options) {
        derivedStateOf {
            backupProgress == null &&
                options.file != null &&
                options.atleastOneChosen
        }
    }
    val cancelEnabled = backupProgress == null

    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        UnpaddedAlertDialogContent(
            title = { Text(text = stringResource(R.string.backup)) },
            text = {
                BackupDialogContent(
                    options = options,
                    backupProgress = backupProgress,
                    onOptionsChange = onOptionsChange,
                    onFileInputClicked = onFileInputClicked,
                )
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        enabled = cancelEnabled,
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = onBackupClick,
                        enabled = backupBtnEnabled,
                    ) {
                        Text(text = stringResource(R.string.backup))
                    }
                }
            },
        )
    }
}

@Composable
private fun BackupDialogContent(
    modifier: Modifier = Modifier,
    options: BackupOptions = BackupOptions(),
    backupProgress: Float? = null,
    onOptionsChange: (BackupOptions) -> Unit = {},
    onFileInputClicked: () -> Unit = {},
) {
    val context = LocalContext.current
    val enabled = backupProgress == null
    val alpha = if (enabled) 1f else DISABLED_ALPHA

    Column(
        modifier = modifier.verticalScroll(state = rememberScrollState()),
    ) {
        BackupOption(
            label = stringResource(R.string.settings),
            checked = options.settings,
            enabled = enabled,
            alpha = alpha,
            onCheckedChange = {
                onOptionsChange(
                    options.copy(
                        settings = it,
                    ),
                )
            },
        )
        BackupOption(
            label = stringResource(R.string.favorites),
            checked = options.favorites,
            enabled = enabled,
            alpha = alpha,
            onCheckedChange = {
                onOptionsChange(
                    options.copy(
                        favorites = it,
                    ),
                )
            },
        )
        BackupOption(
            label = stringResource(R.string.light_dark),
            checked = options.lightDark,
            enabled = enabled,
            alpha = alpha,
            onCheckedChange = {
                onOptionsChange(
                    options.copy(
                        lightDark = it,
                    ),
                )
            },
        )
        BackupOption(
            label = stringResource(R.string.saved_searches),
            checked = options.savedSearches,
            enabled = enabled,
            alpha = alpha,
            onCheckedChange = {
                onOptionsChange(
                    options.copy(
                        savedSearches = it,
                    ),
                )
            },
        )
        BackupOption(
            label = stringResource(R.string.viewed),
            checked = options.viewed,
            enabled = enabled,
            alpha = alpha,
            onCheckedChange = {
                onOptionsChange(
                    options.copy(
                        viewed = it,
                    ),
                )
            },
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        if (upEvent != null) {
                            onFileInputClicked()
                        }
                    }
                }
                .semantics {
                    onClick {
                        onFileInputClicked()
                        true
                    }
                },
            readOnly = true,
            enabled = enabled,
            value = options.file?.run {
                getRealPath(context, this) ?: this.toString()
            } ?: "",
            onValueChange = {},
            singleLine = true,
            label = { Text(text = stringResource(R.string.save_as)) },
            placeholder = { Text(text = stringResource(R.string.tap_to_set_save_location)) },
        )
        if (backupProgress != null) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = stringResource(R.string.backing_up))
                ProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = backupProgress,
                    circular = false,
                )
            }
        }
    }
}

@Composable
private fun BackupOption(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    alpha: Float,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        modifier = Modifier
            .clickable(enabled = enabled) {
                onCheckedChange(!checked)
            }
            .padding(horizontal = 8.dp),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        headlineContent = {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
        },
        leadingContent = {
            Checkbox(
                modifier = Modifier.size(24.dp),
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        },
    )
}

private data class BackupDialogProps(
    val options: BackupOptions = BackupOptions(),
    val backupProgress: Float? = null,
)

private class BackupDialogPPP : CollectionPreviewParameterProvider<BackupDialogProps>(
    listOf(
        BackupDialogProps(),
        BackupDialogProps(
            backupProgress = -1F,
        ),
    ),
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewBackupDialog(
    @PreviewParameter(BackupDialogPPP::class) props: BackupDialogProps,
) {
    var options by remember(props.options) {
        mutableStateOf(props.options)
    }
    var progress by remember(props.backupProgress) {
        mutableStateOf(props.backupProgress)
    }
    val coroutineScope = rememberCoroutineScope()

    WallFlowTheme {
        Surface {
            BackupDialog(
                options = options,
                backupProgress = progress,
                onOptionsChange = { options = it },
                onFileInputClicked = {
                    options = options.copy(
                        file = Uri.EMPTY,
                    )
                },
                onBackupClick = {
                    coroutineScope.launch {
                        progress = -1F
                        delay(5000)
                        progress = null
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreDialog(
    modifier: Modifier = Modifier,
    summary: RestoreSummary? = null,
    options: BackupOptions? = null,
    parsingJson: Boolean = false,
    restoreProgress: Float? = null,
    exception: RestoreException? = null,
    onFileInputClicked: () -> Unit = {},
    onOptionsChange: (BackupOptions) -> Unit = {},
    onRestoreClick: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val restoreBtnEnabled by remember(options, parsingJson) {
        derivedStateOf {
            restoreProgress == null &&
                options?.file != null &&
                !parsingJson &&
                options.atleastOneChosen
        }
    }
    val cancelEnabled = restoreProgress == null

    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        UnpaddedAlertDialogContent(
            title = { Text(text = stringResource(R.string.restore)) },
            text = {
                RestoreDialogContent(
                    options = options,
                    summary = summary,
                    parsingJson = parsingJson,
                    restoreProgress = restoreProgress,
                    exception = exception,
                    onOptionsChange = onOptionsChange,
                    onFileInputClicked = onFileInputClicked,
                )
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        enabled = cancelEnabled,
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = onRestoreClick,
                        enabled = restoreBtnEnabled,
                    ) {
                        Text(text = stringResource(R.string.restore))
                    }
                }
            },
        )
    }
}

@Composable
private fun RestoreDialogContent(
    modifier: Modifier = Modifier,
    options: BackupOptions? = null,
    summary: RestoreSummary? = null,
    parsingJson: Boolean = false,
    restoreProgress: Float? = null,
    exception: RestoreException? = null,
    onOptionsChange: (BackupOptions) -> Unit = {},
    onFileInputClicked: () -> Unit = {},
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .verticalScroll(state = rememberScrollState()),
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        if (upEvent != null) {
                            onFileInputClicked()
                        }
                    }
                }
                .semantics {
                    onClick {
                        onFileInputClicked()
                        true
                    }
                },
            readOnly = true,
            enabled = !parsingJson,
            value = options?.file?.run {
                getRealPath(context, this) ?: this.toString()
            } ?: "",
            onValueChange = {},
            singleLine = true,
            label = { Text(text = stringResource(R.string.backup_file)) },
            placeholder = { Text(text = stringResource(R.string.tap_to_choose_backup_file)) },
            isError = exception != null,
            supportingText = if (exception != null) {
                {
                    Text(
                        text = when (exception) {
                            is InvalidJsonException,
                            is FileNotFoundException,
                            -> {
                                exception.cause?.localizedMessage
                                    ?: exception.localizedMessage
                                    ?: stringResource(R.string.invalid_backup_json)
                            }
                            else -> exception.localizedMessage
                        },
                    )
                }
            } else {
                null
            },
        )
        if (parsingJson) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = stringResource(R.string.reading_file))
                ProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    circular = false,
                )
            }
        }
        if (!parsingJson && options != null && summary != null) {
            val settingsEnabled = restoreProgress == null && summary.settings
            val summaryHasFavs = summary.favorites != null && summary.favorites > 0
            val favoritesEnabled = restoreProgress == null && summaryHasFavs
            val summaryHasLightDark = summary.lightDark != null && summary.lightDark > 0
            val lightDarkEnabled = restoreProgress == null && summaryHasLightDark
            val summaryHasSearches = summary.savedSearches != null && summary.savedSearches > 0
            val savedSearchesEnabled = restoreProgress == null && summaryHasSearches
            val summaryHasViewed = summary.viewed != null && summary.viewed > 0
            val viewedEnabled = restoreProgress == null && summaryHasViewed

            ListItem(
                modifier = Modifier
                    .clickable(enabled = settingsEnabled) {
                        onOptionsChange(
                            options.copy(
                                settings = !options.settings,
                            ),
                        )
                    }
                    .padding(horizontal = 8.dp),
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                headlineContent = {
                    Text(
                        text = stringResource(R.string.settings),
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = getAlpha(settingsEnabled),
                        ),
                    )
                },
                supportingContent = if (!summary.settings) {
                    {
                        Text(
                            text = stringResource(R.string.no_settings_found),
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = DISABLED_ALPHA,
                            ),
                        )
                    }
                } else {
                    null
                },
                leadingContent = {
                    Checkbox(
                        modifier = Modifier.size(24.dp),
                        checked = options.settings,
                        enabled = settingsEnabled,
                        onCheckedChange = {
                            onOptionsChange(
                                options.copy(
                                    settings = it,
                                ),
                            )
                        },
                    )
                },
            )
            RestoreOption(
                label = stringResource(R.string.favorites),
                notFoundSupportingText = stringResource(R.string.no_favorites_found),
                checked = options.favorites,
                enabled = favoritesEnabled,
                summaryHasOption = summaryHasFavs,
                summaryCount = summary.favorites,
                onCheckedChange = {
                    onOptionsChange(
                        options.copy(
                            favorites = it,
                        ),
                    )
                },
            )
            RestoreOption(
                label = stringResource(R.string.light_dark),
                notFoundSupportingText = stringResource(R.string.no_light_dark_found),
                checked = options.lightDark,
                enabled = lightDarkEnabled,
                summaryHasOption = summaryHasLightDark,
                summaryCount = summary.lightDark,
                onCheckedChange = {
                    onOptionsChange(
                        options.copy(
                            lightDark = it,
                        ),
                    )
                },
            )
            RestoreOption(
                label = stringResource(R.string.saved_searches),
                notFoundSupportingText = stringResource(R.string.no_saved_searches_found),
                checked = options.savedSearches,
                enabled = savedSearchesEnabled,
                summaryHasOption = summaryHasSearches,
                summaryCount = summary.savedSearches,
                onCheckedChange = {
                    onOptionsChange(
                        options.copy(
                            savedSearches = it,
                        ),
                    )
                },
            )
            RestoreOption(
                label = stringResource(R.string.viewed),
                notFoundSupportingText = stringResource(R.string.no_viewed_found),
                checked = options.viewed,
                enabled = viewedEnabled,
                summaryHasOption = summaryHasViewed,
                summaryCount = summary.viewed,
                onCheckedChange = {
                    onOptionsChange(
                        options.copy(
                            viewed = it,
                        ),
                    )
                },
            )
        }
        if (restoreProgress != null) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = stringResource(R.string.restoring))
                ProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = restoreProgress,
                    circular = false,
                )
            }
        }
    }
}

@Composable
private fun RestoreOption(
    label: String,
    notFoundSupportingText: String,
    checked: Boolean,
    enabled: Boolean,
    summaryHasOption: Boolean,
    summaryCount: Int?,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        modifier = Modifier
            .clickable(enabled = enabled) {
                onCheckedChange(!checked)
            }
            .padding(horizontal = 8.dp),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        headlineContent = {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = getAlpha(enabled),
                ),
            )
        },
        supportingContent = {
            if (summaryHasOption) {
                Text(
                    text = stringResource(
                        R.string.found_n,
                        summaryCount ?: 0,
                    ),
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = getAlpha(enabled),
                    ),
                )
            } else {
                Text(
                    text = notFoundSupportingText,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = DISABLED_ALPHA,
                    ),
                )
            }
        },
        leadingContent = {
            Checkbox(
                modifier = Modifier.size(24.dp),
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        },
    )
}

private data class RestoreDialogProps(
    val options: BackupOptions? = null,
    val summary: RestoreSummary? = null,
    val parsingJson: Boolean = false,
    val restoreProgress: Float? = null,
    val exception: RestoreException? = null,
)

private class RestoreDialogPPP : CollectionPreviewParameterProvider<RestoreDialogProps>(
    listOf(
        RestoreDialogProps(),
        RestoreDialogProps(
            options = BackupOptions(
                file = Uri.EMPTY,
            ),
            parsingJson = true,
        ),
        RestoreDialogProps(
            options = BackupOptions(
                file = Uri.EMPTY,
            ),
            summary = RestoreSummary(
                file = Uri.EMPTY,
            ),
            parsingJson = false,
        ),
        RestoreDialogProps(
            options = BackupOptions(
                file = Uri.EMPTY,
                settings = true,
            ),
            summary = RestoreSummary(
                file = Uri.EMPTY,
                settings = true,
            ),
            parsingJson = false,
        ),
        RestoreDialogProps(
            options = BackupOptions(
                file = Uri.EMPTY,
                settings = true,
            ),
            summary = RestoreSummary(
                file = Uri.EMPTY,
                settings = true,
            ),
            parsingJson = false,
            exception = InvalidJsonException(),
        ),
        RestoreDialogProps(
            options = BackupOptions(
                file = Uri.EMPTY,
                settings = true,
            ),
            summary = RestoreSummary(
                file = Uri.EMPTY,
                settings = true,
            ),
            parsingJson = false,
            restoreProgress = -1F,
        ),
    ),
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewRestoreDialog(
    @PreviewParameter(RestoreDialogPPP::class) props: RestoreDialogProps,
) {
    WallFlowTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            RestoreDialogContent(
                options = props.options,
                summary = props.summary,
                parsingJson = props.parsingJson,
                restoreProgress = props.restoreProgress,
                exception = props.exception,
            )
        }
    }
}
