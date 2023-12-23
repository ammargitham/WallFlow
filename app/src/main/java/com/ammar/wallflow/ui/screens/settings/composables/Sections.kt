package com.ammar.wallflow.ui.screens.settings.composables

import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider as CPPP
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.DISABLED_ALPHA
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.ObjectDetectionDelegate
import com.ammar.wallflow.data.preferences.ViewedWallpapersLook
import com.ammar.wallflow.data.preferences.defaultAutoWallpaperFreq
import com.ammar.wallflow.model.ObjectDetectionModel
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.ui.common.ProgressIndicator
import com.ammar.wallflow.ui.common.getPaddingValuesConverter
import com.ammar.wallflow.ui.screens.settings.NextRun
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.utils.ExifWriteType
import com.ammar.wallflow.utils.objectdetection.objectsDetector
import com.ammar.wallflow.workers.AutoWallpaperWorker
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimePeriod

@Composable
fun EditSavedSearchBottomSheetHeader(
    name: String = "",
    saveEnabled: Boolean = true,
    nameHasError: Boolean = false,
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
    HorizontalDivider(modifier = Modifier.fillMaxWidth())
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 16.dp,
                start = 22.dp,
                end = 22.dp,
            ),
        label = { Text(text = stringResource(R.string.name)) },
        supportingText = if (nameHasError) {
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
        value = name,
        singleLine = true,
        isError = nameHasError,
        onValueChange = onNameChange,
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewEditSearchBottomSheetHeader() {
    WallFlowTheme {
        Surface {
            Column {
                EditSavedSearchBottomSheetHeader()
            }
        }
    }
}

internal fun LazyListScope.accountSection(
    onWallhavenApiKeyItemClick: () -> Unit = {},
) {
    item { Header(stringResource(R.string.account)) }
    item {
        WallhavenApiKeyItem(
            modifier = Modifier.clickable(onClick = onWallhavenApiKeyItemClick),
        )
    }
}

@Composable
private fun WallhavenApiKeyItem(
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

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAccountSection() {
    WallFlowTheme {
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
    writeTagsToExif: Boolean = false,
    tagsExifWriteType: ExifWriteType = ExifWriteType.APPEND,
    onBlurSketchyCheckChange: (checked: Boolean) -> Unit = {},
    onBlurNsfwCheckChange: (checked: Boolean) -> Unit = {},
    onWriteTagsToExifCheckChange: (checked: Boolean) -> Unit = {},
    onTagsWriteTypeClick: () -> Unit = {},
    onManageSavedSearchesClick: () -> Unit = {},
) {
    item { Header(stringResource(R.string.general)) }
    item {
        ListItem(
            modifier = Modifier.clickable { onBlurSketchyCheckChange(!blurSketchy) },
            headlineContent = { Text(text = stringResource(R.string.blur_sketchy_wallpapers)) },
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
            headlineContent = { Text(text = stringResource(R.string.blur_nsfw_wallpapers)) },
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
            modifier = Modifier.clickable { onWriteTagsToExifCheckChange(!writeTagsToExif) },
            headlineContent = {
                Text(text = stringResource(R.string.write_tags_to_exif))
            },
            supportingContent = {
                Text(text = stringResource(R.string.write_tags_to_exif_desc))
            },
            trailingContent = {
                Switch(
                    modifier = Modifier.height(24.dp),
                    checked = writeTagsToExif,
                    onCheckedChange = onWriteTagsToExifCheckChange,
                )
            },
        )
    }
    item {
        AnimatedVisibility(
            modifier = Modifier.clipToBounds(),
            visible = writeTagsToExif,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            label = "EXIF write type",
        ) {
            ListItem(
                modifier = Modifier.clickable(onClick = onTagsWriteTypeClick),
                headlineContent = {
                    Text(text = stringResource(R.string.exif_write_type))
                },
                supportingContent = {
                    Text(
                        text = stringResource(
                            when (tagsExifWriteType) {
                                ExifWriteType.APPEND -> R.string.append
                                ExifWriteType.OVERWRITE -> R.string.overwrite
                            },
                        ),
                    )
                },
            )
        }
    }
    item {
        ListItem(
            modifier = Modifier.clickable(onClick = onManageSavedSearchesClick),
            headlineContent = { Text(text = stringResource(R.string.manager_saved_searches)) },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewGeneralSection() {
    WallFlowTheme {
        Surface {
            LazyColumn {
                generalSection(
                    writeTagsToExif = true,
                )
            }
        }
    }
}

internal fun LazyListScope.lookAndFeelSection(
    showLocalTab: Boolean = true,
    onThemeClick: () -> Unit = {},
    onLayoutClick: () -> Unit = {},
    onShowLocalTabChange: (Boolean) -> Unit = {},
) {
    item { Header(stringResource(R.string.look_and_feel)) }
    item {
        ListItem(
            modifier = Modifier.clickable(onClick = onThemeClick),
            headlineContent = { Text(text = stringResource(R.string.theme)) },
        )
    }
    item {
        ListItem(
            modifier = Modifier.clickable(onClick = onLayoutClick),
            headlineContent = { Text(text = stringResource(R.string.layout)) },
        )
    }
    item {
        ListItem(
            modifier = Modifier.clickable { onShowLocalTabChange(!showLocalTab) },
            headlineContent = { Text(text = stringResource(R.string.show_local_tab)) },
            trailingContent = {
                Switch(
                    modifier = Modifier.height(24.dp),
                    checked = showLocalTab,
                    onCheckedChange = onShowLocalTabChange,
                )
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewLookAndFeelSection() {
    WallFlowTheme {
        Surface {
            LazyColumn {
                lookAndFeelSection()
            }
        }
    }
}

internal fun LazyListScope.objectDetectionSection(
    enabled: Boolean = false,
    delegate: ObjectDetectionDelegate = ObjectDetectionDelegate.GPU,
    model: ObjectDetectionModel = ObjectDetectionModel.DEFAULT,
    onEnabledChange: (enabled: Boolean) -> Unit = {},
    onDelegateClick: () -> Unit = {},
    onModelClick: () -> Unit = {},
) {
    val alpha = if (enabled) 1f else DISABLED_ALPHA

    item { Header(stringResource(R.string.object_detection)) }
    item {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.object_detection_setting_desc),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    item {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.object_detection_setting_warning),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
        )
    }
    item {
        ListItem(
            modifier = Modifier.clickable { onEnabledChange(!enabled) },
            headlineContent = { Text(text = stringResource(R.string.enable_object_detection)) },
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
                    text = stringResource(R.string.tflite_delegate),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
            },
            supportingContent = {
                Text(
                    text = delegateString(delegate),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
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
                    text = stringResource(R.string.tflite_model),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
            },
            supportingContent = {
                Text(
                    text = model.name,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSubjectDetectionSection() {
    WallFlowTheme {
        Surface {
            LazyColumn {
                objectDetectionSection()
            }
        }
    }
}

internal fun LazyListScope.autoWallpaperSection(
    enabled: Boolean = false,
    sourcesSummary: String? = null,
    crop: Boolean = true,
    useObjectDetection: Boolean = true,
    frequency: DateTimePeriod = defaultAutoWallpaperFreq,
    nextRun: NextRun = NextRun.NotScheduled,
    markFavorite: Boolean = false,
    download: Boolean = false,
    showNotification: Boolean = false,
    autoWallpaperStatus: AutoWallpaperWorker.Companion.Status? = null,
    targets: Set<WallpaperTarget> = setOf(WallpaperTarget.HOME, WallpaperTarget.LOCKSCREEN),
    onEnabledChange: (Boolean) -> Unit = {},
    onSourcesClick: () -> Unit = {},
    onFrequencyClick: () -> Unit = {},
    onUseObjectDetectionChange: (Boolean) -> Unit = {},
    onConstraintsClick: () -> Unit = {},
    onChangeNowClick: () -> Unit = {},
    onNextRunInfoClick: () -> Unit = {},
    onMarkFavoriteChange: (Boolean) -> Unit = {},
    onDownloadChange: (Boolean) -> Unit = {},
    onShowNotificationChange: (Boolean) -> Unit = {},
    onSetToClick: () -> Unit = {},
    onCropChange: (Boolean) -> Unit = {},
) {
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    val objectDetectionAlpha = if (enabled && crop) 1f else DISABLED_ALPHA

    item { Header(stringResource(R.string.auto_wallpaper)) }
    item {
        ListItem(
            modifier = Modifier.clickable { onEnabledChange(!enabled) },
            headlineContent = { Text(text = stringResource(R.string.enable_auto_wallpaper)) },
            trailingContent = {
                Switch(
                    modifier = Modifier.height(24.dp),
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                )
            },
        )
    }
    if (enabled) {
        item {
            val text = when (nextRun) {
                is NextRun.NextRunTime -> {
                    val format = DateFormat.getBestDateTimePattern(
                        Locale.getDefault(),
                        "yyyyy.MMMM.dd hh:mm",
                    )
                    val formatted = DateFormat.format(
                        format,
                        nextRun.instant.toEpochMilliseconds(),
                    ).toString()
                    stringResource(R.string.next_run_at, formatted)
                }
                NextRun.NotScheduled -> stringResource(R.string.not_scheduled)
                NextRun.Running -> stringResource(R.string.running)
            }
            ListItem(
                headlineContent = {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingContent = {
                    IconButton(onClick = onNextRunInfoClick) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.info),
                        )
                    }
                },
            )
        }
    }
    item {
        ListItem(
            modifier = Modifier.clickable(
                enabled = enabled,
                onClick = onSourcesClick,
            ),
            headlineContent = {
                Text(
                    text = stringResource(R.string.sources),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
            },
            supportingContent = if (sourcesSummary?.isNotBlank() == true) {
                {
                    Text(
                        text = sourcesSummary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    )
                }
            } else {
                null
            },
        )
    }
    item {
        ListItem(
            modifier = Modifier.clickable(
                enabled = enabled,
                onClick = onSetToClick,
            ),
            headlineContent = {
                Text(
                    text = stringResource(R.string.set_to),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
            },
            supportingContent = if (targets.isNotEmpty()) {
                {
                    Text(
                        text = getTargetsSummary(targets),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    )
                }
            } else {
                null
            },
        )
    }
    item {
        ListItem(
            modifier = Modifier.clickable(
                enabled = enabled,
                onClick = onFrequencyClick,
            ),
            headlineContent = {
                Text(
                    text = stringResource(R.string.frequency),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
            },
            supportingContent = {
                Text(
                    text = getFrequencyString(frequency),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
            },
        )
    }
    item {
        ListItem(
            modifier = Modifier.clickable(
                enabled = enabled,
                onClick = { onCropChange(!crop) },
            ),
            headlineContent = {
                Text(
                    text = stringResource(R.string.crop),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(R.string.auto_wallpaper_crop_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
            },
            trailingContent = {
                Switch(
                    modifier = Modifier.height(24.dp),
                    enabled = enabled,
                    checked = crop,
                    onCheckedChange = onCropChange,
                )
            },
        )
    }
    if (objectsDetector.isEnabled) {
        item {
            ListItem(
                modifier = Modifier.clickable(
                    enabled = enabled && crop,
                    onClick = { onUseObjectDetectionChange(!useObjectDetection) },
                ),
                headlineContent = {
                    Text(
                        text = stringResource(R.string.use_object_detection),
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = objectDetectionAlpha,
                        ),
                    )
                },
                trailingContent = {
                    Switch(
                        modifier = Modifier.height(24.dp),
                        enabled = enabled && crop,
                        checked = useObjectDetection,
                        onCheckedChange = onUseObjectDetectionChange,
                    )
                },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.use_object_detection_desc),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = objectDetectionAlpha,
                        ),
                    )
                },
            )
        }
    }
    item {
        ListItem(
            modifier = Modifier.clickable(
                enabled = enabled,
                onClick = onConstraintsClick,
            ),
            headlineContent = {
                Text(
                    text = stringResource(R.string.constraints),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(R.string.constraints_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
            },
        )
    }
    item {
        ListItem(
            modifier = Modifier.clickable(
                enabled = enabled,
                onClick = { onMarkFavoriteChange(!markFavorite) },
            ),
            headlineContent = {
                Text(
                    text = stringResource(R.string.mark_as_favorite),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
            },
            trailingContent = {
                Switch(
                    modifier = Modifier.height(24.dp),
                    enabled = enabled,
                    checked = markFavorite,
                    onCheckedChange = onMarkFavoriteChange,
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(R.string.auto_wallpaper_mark_as_favorite_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
            },
        )
    }
    item {
        ListItem(
            modifier = Modifier.clickable(
                enabled = enabled,
                onClick = { onDownloadChange(!download) },
            ),
            headlineContent = {
                Text(
                    text = stringResource(R.string.download),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
            },
            trailingContent = {
                Switch(
                    modifier = Modifier.height(24.dp),
                    enabled = enabled,
                    checked = download,
                    onCheckedChange = onDownloadChange,
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(R.string.auto_wallpaper_download_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
            },
        )
    }
    item {
        ListItem(
            modifier = Modifier.clickable(
                enabled = enabled,
                onClick = { onShowNotificationChange(!showNotification) },
            ),
            headlineContent = {
                Text(
                    text = stringResource(R.string.notification),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
            },
            trailingContent = {
                Switch(
                    modifier = Modifier.height(24.dp),
                    enabled = enabled,
                    checked = showNotification,
                    onCheckedChange = onShowNotificationChange,
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(R.string.auto_wallpaper_notification_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
            },
        )
    }
    item {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            ChangeNowButton(
                autoWallpaperStatus = autoWallpaperStatus,
                enabled = enabled,
                onChangeNowClick = onChangeNowClick,
            )
        }
    }
}

private data class PreviewAutoWallpaperSectionProps(
    val enabled: Boolean = false,
)

private class AutoWallpaperSectionPPP : CPPP<PreviewAutoWallpaperSectionProps>(
    listOf(
        PreviewAutoWallpaperSectionProps(),
        PreviewAutoWallpaperSectionProps(enabled = true),
    ),
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAutoWallpaperSection(
    @PreviewParameter(AutoWallpaperSectionPPP::class) props: PreviewAutoWallpaperSectionProps,
) {
    WallFlowTheme {
        Surface {
            LazyColumn {
                autoWallpaperSection(
                    enabled = props.enabled,
                )
            }
        }
    }
}

internal fun LazyListScope.viewedWallpapersSection(
    enabled: Boolean = false,
    look: ViewedWallpapersLook = ViewedWallpapersLook.DIM_WITH_LABEL,
    onEnabledChange: (Boolean) -> Unit = {},
    onViewedWallpapersLookClick: () -> Unit = {},
    onClearClick: () -> Unit = {},
) {
    item { Header(stringResource(R.string.viewed_wallpapers)) }
    item {
        ListItem(
            modifier = Modifier.clickable {
                onEnabledChange(!enabled)
            },
            headlineContent = { Text(text = stringResource(R.string.remember_viewed_wallpapers)) },
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
            modifier = Modifier.clickable(onClick = onViewedWallpapersLookClick),
            headlineContent = { Text(text = stringResource(R.string.viewed_wallpapers_look)) },
            supportingContent = { Text(text = viewedWallpapersLookString(look)) },
        )
    }
    item {
        Box(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        ) {
            OutlinedButton(onClick = onClearClick) {
                Text(text = stringResource(R.string.clear))
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewViewedWallpapersSection() {
    WallFlowTheme {
        Surface {
            LazyColumn {
                viewedWallpapersSection()
            }
        }
    }
}

@Composable
private fun ChangeNowButton(
    autoWallpaperStatus: AutoWallpaperWorker.Companion.Status? = null,
    enabled: Boolean = true,
    onChangeNowClick: () -> Unit = {},
) {
    val layoutDirection = LocalLayoutDirection.current
    val changing = autoWallpaperStatus?.isSuccessOrFail() == false
    val contentPadding by animateValueAsState(
        targetValue = if (changing) {
            ButtonDefaults.ButtonWithIconContentPadding
        } else {
            ButtonDefaults.ContentPadding
        },
        typeConverter = getPaddingValuesConverter(layoutDirection),
        animationSpec = remember { tween(delayMillis = 200) },
        label = "contentPadding",
    )
    OutlinedButton(
        enabled = if (!enabled) {
            false
        } else {
            !changing
        },
        contentPadding = contentPadding,
        onClick = onChangeNowClick,
    ) {
        AnimatedVisibility(visible = changing) {
            Row(Modifier.clearAndSetSemantics {}) {
                ProgressIndicator(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            }
        }
        Text(text = stringResource(R.string.change_now))
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewChangeNowButton() {
    WallFlowTheme {
        Surface {
            val coroutineScope = rememberCoroutineScope()
            var status: AutoWallpaperWorker.Companion.Status? by remember {
                mutableStateOf(null)
            }
            Column {
                ChangeNowButton(
                    autoWallpaperStatus = status,
                    onChangeNowClick = {
                        status = AutoWallpaperWorker.Companion.Status.Pending
                        coroutineScope.launch {
                            delay(5000)
                            status = null
                        }
                    },
                )
                ChangeNowButton(
                    autoWallpaperStatus = AutoWallpaperWorker.Companion.Status.Pending,
                )
                ChangeNowButton(
                    autoWallpaperStatus = AutoWallpaperWorker.Companion.Status.Running,
                )
                ChangeNowButton(
                    autoWallpaperStatus = AutoWallpaperWorker.Companion.Status.Success,
                )
                ChangeNowButton(
                    autoWallpaperStatus = AutoWallpaperWorker.Companion.Status.Failed(),
                )
            }
        }
    }
}

@Composable
private fun Header(text: String) {
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
    item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }
}
