package com.ammar.havenwalls.ui.settings.composables

import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.havenwalls.DISABLED_ALPHA
import com.ammar.havenwalls.R
import com.ammar.havenwalls.data.preferences.defaultAutoWallpaperFreq
import com.ammar.havenwalls.model.ObjectDetectionModel
import com.ammar.havenwalls.ui.settings.NextRun
import com.ammar.havenwalls.ui.theme.HavenWallsTheme
import java.util.Locale
import kotlinx.datetime.DateTimePeriod
import org.tensorflow.lite.task.core.ComputeSettings.Delegate

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

internal fun LazyListScope.accountSection(
    onWallhavenApiKeyItemClick: () -> Unit = {},
) {
    item { Header(stringResource(R.string.account)) }
    item {
        WallhavenApiKeyItem(
            modifier = Modifier.clickable(onClick = onWallhavenApiKeyItemClick)
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

    item { Header(stringResource(R.string.object_detection)) }
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
            },
            supportingContent = {
                Text(
                    text = delegateString(delegate),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
            },
            supportingContent = {
                Text(
                    text = model.name,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            },
        )
    }
}

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

internal fun LazyListScope.autoWallpaperSection(
    enabled: Boolean = false,
    savedSearchName: String? = null,
    useObjectDetection: Boolean = true,
    frequency: DateTimePeriod = defaultAutoWallpaperFreq,
    nextRun: NextRun = NextRun.NotScheduled,
    showNotification: Boolean = false,
    onEnabledChange: (Boolean) -> Unit = {},
    onSavedSearchClick: () -> Unit = {},
    onFrequencyClick: () -> Unit = {},
    onUseObjectDetectionChange: (Boolean) -> Unit = {},
    onConstraintsClick: () -> Unit = {},
    onChangeNowClick: () -> Unit = {},
    onNextRunInfoClick: () -> Unit = {},
    onShowNotificationChange: (Boolean) -> Unit = {},
) {
    val alpha = if (enabled) 1f else DISABLED_ALPHA

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
                        "yyyyy.MMMM.dd hh:mm"
                    )
                    val formatted = DateFormat.format(
                        format,
                        nextRun.instant.toEpochMilliseconds()
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
                }
            )
        }
    }
    item {
        ListItem(
            modifier = Modifier.clickable(
                enabled = enabled,
                onClick = onSavedSearchClick,
            ),
            headlineContent = {
                Text(
                    text = stringResource(R.string.search_to_use),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
            },
            supportingContent = if (savedSearchName?.isNotBlank() == true) {
                {
                    Text(
                        text = savedSearchName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    )
                }
            } else null,
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
                    text = pluralStringResource(
                        R.plurals.every_x_hrs,
                        frequency.hours,
                        frequency.hours,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
            },
        )
    }
    item {
        ListItem(
            modifier = Modifier.clickable(
                enabled = enabled,
                onClick = { onUseObjectDetectionChange(!useObjectDetection) },
            ),
            headlineContent = {
                Text(
                    text = stringResource(R.string.use_object_detection),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
            },
            trailingContent = {
                Switch(
                    modifier = Modifier.height(24.dp),
                    enabled = enabled,
                    checked = useObjectDetection,
                    onCheckedChange = onUseObjectDetectionChange,
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(R.string.use_object_detection_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
            },
        )
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
            OutlinedButton(
                enabled = enabled,
                onClick = onChangeNowClick,
            ) {
                Text(text = stringResource(R.string.change_now))
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAutoWallpaperSection() {
    HavenWallsTheme {
        Surface {
            LazyColumn {
                autoWallpaperSection()
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAutoWallpaperSectionEnabled() {
    HavenWallsTheme {
        Surface {
            LazyColumn {
                autoWallpaperSection(
                    enabled = true,
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
    item { Divider(modifier = Modifier.fillMaxWidth()) }
}
