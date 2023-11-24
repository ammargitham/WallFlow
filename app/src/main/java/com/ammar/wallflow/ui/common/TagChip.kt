package com.ammar.wallflow.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3x.AssistChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.ui.theme.WallFlowTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.Clock

@Composable
fun TagChip(
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    wallhavenTag: WallhavenTag,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val viewConfiguration = LocalViewConfiguration.current

    LaunchedEffect(interactionSource) {
        var isLongClick = false

        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isLongClick = false
                    delay(viewConfiguration.longPressTimeoutMillis)
                    isLongClick = true
                    onLongClick()
                }

                is PressInteraction.Release -> {
                    if (!isLongClick) {
                        onClick()
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier.width(IntrinsicSize.Max),
    ) {
        AssistChip(
            onClick = {},
            interactionSource = interactionSource,
            label = { Text(text = "#${wallhavenTag.name}") },
        )
        if (loading) {
            PlaceholderChip(
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewTagChip() {
    WallFlowTheme {
        Surface {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TagChip(
                    loading = true,
                    wallhavenTag = WallhavenTag(
                        id = 1,
                        name = "Test-1",
                        alias = emptyList(),
                        categoryId = 1,
                        category = "",
                        purity = Purity.SFW,
                        createdAt = Clock.System.now(),
                    ),
                )
                TagChip(
                    wallhavenTag = WallhavenTag(
                        id = 1,
                        name = "Test-1",
                        alias = emptyList(),
                        categoryId = 1,
                        category = "",
                        purity = Purity.SFW,
                        createdAt = Clock.System.now(),
                    ),
                )
            }
        }
    }
}
