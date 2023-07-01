package com.ammar.havenwalls.ui.common

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.rounded.List
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.havenwalls.R
import com.ammar.havenwalls.ui.theme.HavenWallsTheme

@Composable
fun <T> SegmentedButtons(
    modifier: Modifier = Modifier,
    options: List<SegmentedButtonOption<T>>,
    mode: SegmentedButtonsMode = SegmentedButtonsMode.MULTI_SELECT,
    value: Set<T>,
    containerColor: Color = Color.Transparent,
    containerColorChecked: Color = MaterialTheme.colorScheme.secondaryContainer,
    enabled: Boolean = true,
    onChange: (values: Set<T>) -> Unit = {},
) {
    try {
        require(options.size > 1) { "IconToggleButtonGroup requires at-least 2 options" }
    } catch (e: Exception) {
        Log.w("IconToggleButtonGroup", "", e)
    }

    if (mode == SegmentedButtonsMode.SINGLE_SELECT) {
        try {
            require(value.size == 1) { "Only 1 value is allowed for SINGLE_SELECT mode" }
        } catch (e: Exception) {
            Log.w("IconToggleButtonGroup", "", e)
        }
    }

    Row(
        modifier = modifier
    ) {
        options.mapIndexed { index, option ->
            require(option.text != null || option.icon != null) {
                "Require either text or icon"
            }

            val isChecked = option.value in value
            val currentEnabled = enabled && option.enabled
            val currentContainerColor by animateColorAsState(
                if (isChecked) containerColorChecked else containerColor
            )

            val optionModifier = when (index) {
                0 -> {
                    if (isChecked) {
                        Modifier
                            .offset(0.dp, 0.dp)
                        // .zIndex(1f)
                    } else {
                        Modifier
                            .offset(0.dp, 0.dp)
                        // .zIndex(0f)
                    }
                }

                else -> {
                    val offset = -1 * index
                    if (isChecked) {
                        Modifier
                            .offset(offset.dp, 0.dp)
                        // .zIndex(1f)
                    } else {
                        Modifier
                            .offset(offset.dp, 0.dp)
                        // .zIndex(0f)
                    }
                }
            }.weight(1f)

            val shape = when (index) {
                // left outer button
                0 -> RoundedCornerShape(
                    topStartPercent = 50,
                    topEndPercent = 0,
                    bottomStartPercent = 50,
                    bottomEndPercent = 0,
                )
                // right outer button
                options.size - 1 -> RoundedCornerShape(
                    topStartPercent = 0,
                    topEndPercent = 50,
                    bottomStartPercent = 0,
                    bottomEndPercent = 50,
                )
                // middle button
                else -> RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            }

            val contentColor = when {
                isChecked -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }

            OutlinedButton(
                modifier = optionModifier,
                shape = shape,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = currentContainerColor,
                    contentColor = contentColor,
                ),
                enabled = currentEnabled,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                onClick = {
                    onChange(
                        if (mode == SegmentedButtonsMode.SINGLE_SELECT) {
                            setOf(option.value)
                        } else {
                            if (isChecked) {
                                // remove from values
                                value.filter { it != option.value }.toSet()
                            } else {
                                // add to values
                                value + option.value
                            }
                        }
                    )
                },
                content = {
                    if (isChecked) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Outlined.Check,
                            contentDescription = stringResource(R.string.selected)
                        )
                        Spacer(modifier = Modifier.requiredWidth(4.dp))
                    }
                    if (!isChecked || option.text == null) {
                        option.icon?.run {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                painter = this(),
                                contentDescription = "",
                            )
                        }
                    }
                    if (!isChecked && option.icon != null && option.text != null) {
                        Spacer(modifier = Modifier.requiredWidth(4.dp))
                    }
                    option.text?.run {
                        Text(
                            text = this,
                            overflow = TextOverflow.Clip,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
            )
        }
    }
}

enum class SegmentedButtonsMode {
    MULTI_SELECT,
    SINGLE_SELECT,
}

data class SegmentedButtonOption<T>(
    val value: T,
    val enabled: Boolean = true,
    val icon: (@Composable () -> Painter)? = null,
    val text: String? = null,
)

private val tempToggleOptions: List<SegmentedButtonOption<String>> = listOf(
    SegmentedButtonOption(
        "First",
        text = "First",
    ),
    SegmentedButtonOption(
        "Second",
        icon = { rememberVectorPainter(Icons.Rounded.List) },
        text = "Second",
    ),
    SegmentedButtonOption(
        "Third",
        icon = { rememberVectorPainter(Icons.Rounded.List) },
    ),
    SegmentedButtonOption(
        "Fourth",
        false,
        icon = { rememberVectorPainter(Icons.Rounded.List) },
    )
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewButtonToggle() {
    HavenWallsTheme {
        Surface {
            SegmentedButtons(
                options = tempToggleOptions,
                value = setOf("First", "Third"),
            )
        }
    }
}
