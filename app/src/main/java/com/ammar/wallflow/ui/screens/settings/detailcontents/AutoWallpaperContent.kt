package com.ammar.wallflow.ui.screens.settings.detailcontents

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.DISABLED_ALPHA
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.defaultAutoWallpaperFreq
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.ui.common.ProgressIndicator
import com.ammar.wallflow.ui.common.getPaddingValuesConverter
import com.ammar.wallflow.ui.screens.settings.NextRun
import com.ammar.wallflow.ui.screens.settings.SettingsExtraType
import com.ammar.wallflow.ui.screens.settings.composables.SettingsDetailListItem
import com.ammar.wallflow.ui.screens.settings.composables.getFrequencyString
import com.ammar.wallflow.ui.screens.settings.composables.getNextRunString
import com.ammar.wallflow.ui.screens.settings.composables.getTargetsSummary
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.utils.objectdetection.objectsDetector
import com.ammar.wallflow.workers.AutoWallpaperWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimePeriod

@Composable
internal fun AutoWallpaperContent(
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    sourcesSummary: String? = null,
    crop: Boolean = true,
    useObjectDetection: Boolean = true,
    useSameFrequency: Boolean = true,
    frequency: DateTimePeriod = defaultAutoWallpaperFreq,
    lsFrequency: DateTimePeriod = defaultAutoWallpaperFreq,
    nextRun: NextRun = NextRun.NotScheduled,
    lsNextRun: NextRun = NextRun.NotScheduled,
    markFavorite: Boolean = false,
    download: Boolean = false,
    showNotification: Boolean = false,
    autoWallpaperStatus: AutoWallpaperWorker.Companion.Status? = null,
    targets: Set<WallpaperTarget> = setOf(WallpaperTarget.HOME, WallpaperTarget.LOCKSCREEN),
    selectedType: SettingsExtraType? = null,
    isExpanded: Boolean = false,
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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        val alpha = if (enabled) 1f else DISABLED_ALPHA
        val objectDetectionAlpha = if (enabled && crop) 1f else DISABLED_ALPHA

        item {
            SettingsDetailListItem(
                modifier = Modifier.clickable { onEnabledChange(!enabled) },
                isExpanded = isExpanded,
                isFirst = true,
                isLast = true,
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
                ListItem(
                    modifier = Modifier.animateItem(),
                    headlineContent = {
                        Text(
                            text = getNextRunString(
                                useSameFrequency = useSameFrequency,
                                nextRun = nextRun,
                                lsNextRun = lsNextRun,
                            ),
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
                    colors = ListItemDefaults.colors(
                        containerColor = if (isExpanded) {
                            MaterialTheme.colorScheme.surfaceContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ),
                )
            }
        } else if (isExpanded) {
            item {
                Spacer(
                    modifier = Modifier
                        .requiredHeight(8.dp)
                        .animateItem(),
                )
            }
        }
        item {
            SettingsDetailListItem(
                modifier = Modifier.clickable(
                    enabled = enabled,
                    onClick = onSourcesClick,
                ),
                isExpanded = isExpanded,
                isFirst = isExpanded,
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
                selected = selectedType == SettingsExtraType.AUTO_WALLPAPER_SOURCES,
            )
        }
        item {
            SettingsDetailListItem(
                modifier = Modifier.clickable(
                    enabled = enabled,
                    onClick = onSetToClick,
                ),
                isExpanded = isExpanded,
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
            SettingsDetailListItem(
                modifier = Modifier.clickable(
                    enabled = enabled,
                    onClick = onFrequencyClick,
                ),
                isExpanded = isExpanded,
                headlineContent = {
                    Text(
                        text = stringResource(R.string.frequency),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    )
                },
                supportingContent = {
                    Text(
                        text = getFrequencyString(
                            useSameFrequency = useSameFrequency,
                            frequency = frequency,
                            lsFrequency = lsFrequency,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    )
                },
            )
        }
        item {
            SettingsDetailListItem(
                modifier = Modifier.clickable(
                    enabled = enabled,
                    onClick = { onCropChange(!crop) },
                ),
                isExpanded = isExpanded,
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
                SettingsDetailListItem(
                    modifier = Modifier.clickable(
                        enabled = enabled && crop,
                        onClick = { onUseObjectDetectionChange(!useObjectDetection) },
                    ),
                    isExpanded = isExpanded,
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
            SettingsDetailListItem(
                modifier = Modifier.clickable(
                    enabled = enabled,
                    onClick = onConstraintsClick,
                ),
                isExpanded = isExpanded,
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
            SettingsDetailListItem(
                modifier = Modifier.clickable(
                    enabled = enabled,
                    onClick = { onMarkFavoriteChange(!markFavorite) },
                ),
                isExpanded = isExpanded,
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
            SettingsDetailListItem(
                modifier = Modifier.clickable(
                    enabled = enabled,
                    onClick = { onDownloadChange(!download) },
                ),
                isExpanded = isExpanded,
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
            SettingsDetailListItem(
                modifier = Modifier.clickable(
                    enabled = enabled,
                    onClick = { onShowNotificationChange(!showNotification) },
                ),
                isExpanded = isExpanded,
                isLast = true,
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
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                ),
            ) {
                ChangeNowButton(
                    autoWallpaperStatus = autoWallpaperStatus,
                    enabled = enabled,
                    onChangeNowClick = onChangeNowClick,
                )
            }
        }
    }
}

private data class PreviewAutoWallpaperContentProps(
    val enabled: Boolean = false,
)

private class AutoWallpaperContentPPP :
    CollectionPreviewParameterProvider<PreviewAutoWallpaperContentProps>(
        listOf(
            PreviewAutoWallpaperContentProps(),
            PreviewAutoWallpaperContentProps(enabled = true),
        ),
    )

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAutoWallpaperContent(
    @PreviewParameter(AutoWallpaperContentPPP::class) props: PreviewAutoWallpaperContentProps,
) {
    WallFlowTheme {
        Surface {
            AutoWallpaperContent(
                enabled = props.enabled,
            )
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
