package com.ammar.wallflow.ui.screens.crop

import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.Display
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ammar.wallflow.R
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.extensions.capitalise
import com.ammar.wallflow.model.Detection
import com.ammar.wallflow.model.DetectionWithBitmap
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.ui.common.AdaptiveBottomSheet
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.utils.DownloadStatus

@Composable
internal fun Actions(
    modifier: Modifier = Modifier,
    objectDetectionEnabled: Boolean = false,
    modelDownloadStatus: DownloadStatus? = null,
    detections: Resource<List<DetectionWithBitmap>> = Resource.Success(emptyList()),
    onSetClick: (Set<WallpaperTarget>) -> Unit = {},
    onCancelClick: () -> Unit = {},
    onDetectionsClick: () -> Unit = {},
) {
    val detectionCount = when (detections) {
        is Resource.Loading -> -1
        is Resource.Success -> detections.data.size
        else -> 0
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            if (objectDetectionEnabled) {
                val containerColor =
                    if (detections !is Resource.Error) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                FilledTonalButton(
                    onClick = onDetectionsClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        disabledContainerColor = containerColor.copy(
                            alpha = 0.5f,
                        ),
                    ),
                    enabled = detectionCount > 0,
                ) {
                    val text = when {
                        modelDownloadStatus is DownloadStatus.Running -> stringResource(
                            R.string.downloading_model,
                        )
                        detections is Resource.Error -> stringResource(
                            R.string.error_detecting_objects,
                        )
                        else -> when (detectionCount) {
                            -1 -> stringResource(R.string.detecting)
                            0 -> stringResource(R.string.zero_detected_objects)
                            else -> pluralStringResource(
                                R.plurals.num_detected_objects,
                                detectionCount,
                                detectionCount,
                            )
                        }
                    }
                    Text(
                        text = text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.requiredWidth(8.dp))
        Column {
            Button(onClick = onCancelClick) {
                Text(
                    text = stringResource(R.string.cancel),
                    maxLines = 1,
                )
            }
        }
        Spacer(modifier = Modifier.requiredWidth(8.dp))
        Column {
            SetButton(
                onHomeScreenClick = { onSetClick(setOf(WallpaperTarget.HOME)) },
                onLockScreenClick = { onSetClick(setOf(WallpaperTarget.LOCKSCREEN)) },
                onBothClick = {
                    onSetClick(setOf(WallpaperTarget.HOME, WallpaperTarget.LOCKSCREEN))
                },
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(widthDp = 500)
@Composable
private fun PreviewActions() {
    WallFlowTheme {
        Surface {
            Actions(
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
    }
}

@Composable
private fun SetButton(
    modifier: Modifier = Modifier,
    showTargetOptions: Boolean = true,
    onHomeScreenClick: () -> Unit = {},
    onLockScreenClick: () -> Unit = {},
    onBothClick: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    Button(
        modifier = modifier,
        onClick = {
            if (showTargetOptions) {
                expanded = true
                return@Button
            }
            onBothClick()
        },
    ) {
        Text(
            text = stringResource(R.string.set),
            maxLines = 1,
        )
    }
    DropdownMenu(
        modifier = Modifier.widthIn(min = 150.dp),
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.home_screen)) },
            onClick = {
                expanded = false
                onHomeScreenClick()
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.lock_screen)) },
            onClick = {
                expanded = false
                onLockScreenClick()
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.both)) },
            onClick = {
                expanded = false
                onBothClick()
            },
        )
    }
}

@Composable
internal fun DetectionsBottomSheet(
    modifier: Modifier = Modifier,
    detections: List<DetectionWithBitmap> = emptyList(),
    onDetectionClick: (detection: DetectionWithBitmap) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    AdaptiveBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        DetectionsBottomSheetContent(
            detections = detections,
            onDetectionClick = onDetectionClick,
        )
    }
}

@Composable
internal fun DetectionsBottomSheetContent(
    modifier: Modifier = Modifier,
    detections: List<DetectionWithBitmap> = emptyList(),
    onDetectionClick: (detection: DetectionWithBitmap) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        items(detections) {
            DetectionsItem(
                detection = it,
                onClick = { onDetectionClick(it) },
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewDetectionsBottomSheetContent() {
    WallFlowTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            DetectionsBottomSheetContent(
                detections = List(5) {
                    DetectionWithBitmap(
                        Detection.EMPTY,
                        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                    )
                },
            )
        }
    }
}

@Composable
internal fun DetectionsItem(
    modifier: Modifier = Modifier,
    detection: DetectionWithBitmap,
    onClick: () -> Unit = {},
) {
    val category = detection.detection.categories.firstOrNull()
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        headlineContent = {
            Text(text = category?.label?.capitalise() ?: stringResource(R.string.entity))
        },
        supportingContent = if (category != null) {
            { Text(text = stringResource(R.string.confidence_score, category.score)) }
        } else {
            null
        },
        leadingContent = {
            AsyncImage(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RectangleShape),
                model = detection.bitmap,
                contentDescription = "",
                contentScale = ContentScale.Fit,
            )
        },
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDetectionItem() {
    WallFlowTheme {
        Surface {
            DetectionsItem(
                detection = DetectionWithBitmap(
                    Detection.EMPTY,
                    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                ),
            )
        }
    }
}

@Composable
internal fun DisplayButton(
    modifier: Modifier = Modifier,
    selectedDisplay: Display? = null,
    displays: List<Display> = emptyList(),
    onChange: (Display) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.wrapContentSize(Alignment.TopStart),
    ) {
        FilledTonalButton(
            modifier = modifier,
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = 8.dp,
            ),
            onClick = { expanded = true },
        ) {
            Text(
                text = selectedDisplay?.name ?: stringResource(R.string.select_display),
                maxLines = 1,
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Icon(
                modifier = Modifier.size(ButtonDefaults.IconSize),
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = stringResource(R.string.add_resolution),
            )
        }
        DropdownMenu(
            modifier = Modifier.widthIn(min = 150.dp),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            displays.map {
                DropdownMenuItem(
                    text = { Text(text = it.name) },
                    leadingIcon = {
                        RadioButton(
                            modifier = Modifier.size(24.dp),
                            selected = selectedDisplay?.displayId == it.displayId,
                            onClick = {
                                expanded = false
                                onChange(it)
                            },
                        )
                    },
                    onClick = {
                        expanded = false
                        onChange(it)
                    },
                )
            }
        }
    }
}

@Composable
internal fun TopActions(
    modifier: Modifier = Modifier,
    crop: Boolean = true,
    displays: List<Display> = emptyList(),
    selectedDisplay: Display? = null,
    onCropChange: (Boolean) -> Unit = {},
    setSelectedDisplay: (Display) -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        if (displays.size > 1) {
            DisplayButton(
                selectedDisplay = selectedDisplay,
                displays = displays,
                onChange = setSelectedDisplay,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Surface(
            modifier = Modifier.indication(
                interactionSource = interactionSource,
                indication = ripple(
                    color = Color.White,
                    bounded = false,
                ),
            ),
            interactionSource = interactionSource,
            color = Color.Transparent,
            shape = ButtonDefaults.shape,
            onClick = { onCropChange(!crop) },
        ) {
            Row(
                modifier = Modifier.padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Checkbox(
                    colors = CheckboxDefaults.colors(
                        uncheckedColor = Color.White,
                    ),
                    checked = crop,
                    onCheckedChange = onCropChange,
                )
                Text(
                    text = stringResource(R.string.crop),
                    color = Color.White,
                )
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewTopActions() {
    WallFlowTheme {
        Surface {
            TopActions(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp),
            )
        }
    }
}
