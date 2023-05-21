package com.ammar.havenwalls.ui.crop

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ammar.havenwalls.R
import com.ammar.havenwalls.data.repository.utils.Resource
import com.ammar.havenwalls.extensions.capitalise
import com.ammar.havenwalls.model.DetectionWithBitmap
import com.ammar.havenwalls.ui.theme.HavenWallsTheme
import com.ammar.havenwalls.utils.DownloadStatus
import org.tensorflow.lite.task.vision.detector.Detection

@Composable
internal fun Actions(
    modifier: Modifier = Modifier,
    objectDetectionEnabled: Boolean = false,
    modelDownloadStatus: DownloadStatus? = null,
    detections: Resource<List<DetectionWithBitmap>> = Resource.Success(emptyList()),
    onSetClick: () -> Unit = {},
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
                    if (detections !is Resource.Error) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                FilledTonalButton(
                    onClick = onDetectionsClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        disabledContainerColor = containerColor.copy(
                            alpha = 0.5f
                        )
                    ),
                    enabled = detectionCount > 0,
                ) {
                    val text = when {
                        modelDownloadStatus is DownloadStatus.Running -> stringResource(R.string.downloading_model)
                        detections is Resource.Error -> "Error detecting objects"
                        else -> when (detectionCount) {
                            -1 -> stringResource(R.string.detecting)
                            0 -> stringResource(R.string.zero_detected_objects)
                            else -> pluralStringResource(
                                R.plurals.num_detected_objects,
                                detectionCount,
                                detectionCount
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
            Button(onClick = onSetClick) {
                Text(
                    text = stringResource(R.string.set),
                    maxLines = 1,
                )
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(widthDp = 500)
@Composable
private fun PreviewActions() {
    HavenWallsTheme {
        Surface {
            Actions(
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetectionsBottomSheet(
    modifier: Modifier = Modifier,
    detections: List<DetectionWithBitmap> = emptyList(),
    onDetectionClick: (detection: DetectionWithBitmap) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val bottomSheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = bottomSheetState,
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
                onClick = { onDetectionClick(it) }
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
    val category = detection.detection.categories?.firstOrNull()
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(text = category?.label?.capitalise() ?: "Entity")
        },
        supportingContent = if (category != null) {
            { Text(text = stringResource(R.string.confidence_score, category.score)) }
        } else null,
        leadingContent = {
            AsyncImage(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RectangleShape),
                model = detection.bitmap,
                contentDescription = "",
                contentScale = ContentScale.Fit,
            )
        }
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDetectionItem() {
    HavenWallsTheme {
        Surface {
            DetectionsItem(
                detection = DetectionWithBitmap(
                    Detection.create(
                        RectF(0f, 0f, 0f, 0f),
                        emptyList(),
                    ),
                    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                )
            )
        }
    }
}
