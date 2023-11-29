package com.ammar.wallflow.ui.screens.settings.composables

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.ViewedWallpapersLook
import com.ammar.wallflow.extensions.toPxF
import com.ammar.wallflow.ui.common.CardFavoriteButton
import com.ammar.wallflow.ui.common.CardLabel
import com.ammar.wallflow.ui.common.CardViewedIcon
import com.ammar.wallflow.ui.common.UnpaddedAlertDialogContent
import com.ammar.wallflow.ui.theme.WallFlowTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewedWallpapersLookOptionsDialog(
    modifier: Modifier = Modifier,
    selectedViewedWallpapersLook: ViewedWallpapersLook = ViewedWallpapersLook.DIM_WITH_LABEL,
    onSaveClick: (viewedWallpapersLook: ViewedWallpapersLook) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    var localSelectedViewedWallpapersLook by remember(selectedViewedWallpapersLook) {
        mutableStateOf(selectedViewedWallpapersLook)
    }
    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        UnpaddedAlertDialogContent(
            title = { Text(text = stringResource(R.string.viewed_wallpapers_look)) },
            text = {
                ViewedWallpapersLookOptionsContent(
                    selectedViewedWallpapersLook = localSelectedViewedWallpapersLook,
                    onOptionClick = { localSelectedViewedWallpapersLook = it },
                )
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(onClick = { onSaveClick(localSelectedViewedWallpapersLook) }) {
                        Text(text = stringResource(R.string.save))
                    }
                }
            },
        )
    }
}

@Composable
private fun ViewedWallpapersLookOptionsContent(
    modifier: Modifier = Modifier,
    selectedViewedWallpapersLook: ViewedWallpapersLook = ViewedWallpapersLook.DIM_WITH_LABEL,
    onOptionClick: (senWallpapersLook: ViewedWallpapersLook) -> Unit = {},
) {
    val cornerRadius = 12.dp
    val cornerRadiusPx = cornerRadius.toPxF()
    val scrollState = rememberScrollState()

    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .width(maxWidth / 1.5f)
                    .align(Alignment.Center),
            ) {
                AsyncImage(
                    modifier = Modifier
                        .clip(RoundedCornerShape(cornerRadius))
                        .fillMaxHeight()
                        .heightIn(max = 300.dp)
                        .drawWithContent {
                            drawContent()
                            if (
                                selectedViewedWallpapersLook in setOf(
                                    ViewedWallpapersLook.DIM,
                                    ViewedWallpapersLook.DIM_WITH_LABEL,
                                    ViewedWallpapersLook.DIM_WITH_ICON,
                                )
                            ) {
                                drawRect(Color.Black.copy(alpha = 0.5f))
                            }
                        },
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("file:///android_asset/example_image.jpg")
                        .transformations(RoundedCornersTransformation(cornerRadiusPx))
                        .build(),
                    contentDescription = "example image",
                    placeholder = ColorPainter(MaterialTheme.colorScheme.primary),
                )

                if (
                    selectedViewedWallpapersLook in setOf(
                        ViewedWallpapersLook.DIM_WITH_LABEL,
                        ViewedWallpapersLook.LABEL,
                    )
                ) {
                    CardLabel(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(
                                start = 8.dp,
                                bottom = 8.dp,
                            ),
                        text = stringResource(R.string.viewed),
                    )
                }

                if (
                    selectedViewedWallpapersLook in setOf(
                        ViewedWallpapersLook.DIM_WITH_ICON,
                        ViewedWallpapersLook.ICON,
                    )
                ) {
                    CardViewedIcon(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(
                                start = 8.dp,
                                bottom = 8.dp,
                            ),
                    )
                }

                CardFavoriteButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            bottom = 4.dp,
                            end = 4.dp,
                        ),
                )
            }
        }
        HorizontalDivider()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(state = scrollState),
        ) {
            ViewedWallpapersLook.entries.map {
                ListItem(
                    modifier = Modifier
                        .clickable(onClick = { onOptionClick(it) })
                        .padding(horizontal = 8.dp),
                    headlineContent = { Text(text = viewedWallpapersLookString(it)) },
                    leadingContent = {
                        RadioButton(
                            modifier = Modifier.size(24.dp),
                            selected = selectedViewedWallpapersLook == it,
                            onClick = { onOptionClick(it) },
                        )
                    },
                )
            }
        }
        HorizontalDivider()
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewViewedWallpapersLookOptionsDialog() {
    WallFlowTheme {
        Surface {
            ViewedWallpapersLookOptionsDialog()
        }
    }
}
