package com.ammar.wallflow.ui.screens.wallpaper

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberPlainTooltipPositionProvider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.model.LightDarkType
import com.ammar.wallflow.model.isDark
import com.ammar.wallflow.model.isExtraDim
import com.ammar.wallflow.model.isLight
import com.ammar.wallflow.model.isUnspecified
import com.ammar.wallflow.ui.theme.WallFlowTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightDarkButton(
    modifier: Modifier = Modifier,
    typeFlags: Int = LightDarkType.UNSPECIFIED,
    onFlagsChange: (Int) -> Unit = {},
    onShowLightDarkInfoClick: () -> Unit = {},
) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    TooltipBox(
        modifier = modifier.wrapContentSize(Alignment.TopStart),
        positionProvider = rememberPlainTooltipPositionProvider(),
        state = rememberTooltipState(),
        tooltip = {
            PlainTooltip {
                Text(text = stringResource(R.string.mark_light_dark))
            }
        },
    ) {
        IconButton(
            modifier = modifier.wrapContentSize(Alignment.TopStart),
            onClick = { expanded = true },
        ) {
            AnimatedContent(
                targetState = typeFlags,
                label = "LightDarkIcon",
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) {
                Icon(
                    painter = painterResource(
                        when {
                            it.isLight() -> R.drawable.baseline_light_mode_24
                            it.isExtraDim() -> R.drawable.dark_extra_dim
                            it.isDark() -> R.drawable.baseline_dark_mode_24
                            else -> R.drawable.baseline_light_dark
                        },
                    ),
                    contentDescription = stringResource(R.string.light_dark),
                )
            }
        }
        DropdownMenu(
            modifier = Modifier
                .widthIn(min = 150.dp)
                .semantics {
                    contentDescription = context.getString(R.string.menu)
                },
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.mark_light_dark),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 16.dp),
                )
                Icon(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        onClick = onShowLightDarkInfoClick,
                    ),
                    painter = painterResource(R.drawable.outline_help_outline_24),
                    contentDescription = stringResource(R.string.help),
                )
            }
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.none)) },
                onClick = { onFlagsChange(LightDarkType.UNSPECIFIED) },
                leadingIcon = {
                    RadioButton(
                        modifier = Modifier.size(24.dp),
                        selected = typeFlags.isUnspecified(),
                        onClick = { onFlagsChange(LightDarkType.UNSPECIFIED) },
                    )
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.light)) },
                onClick = { onFlagsChange(LightDarkType.LIGHT) },
                leadingIcon = {
                    RadioButton(
                        modifier = Modifier.size(24.dp),
                        selected = typeFlags.isLight(),
                        onClick = { onFlagsChange(LightDarkType.LIGHT) },
                    )
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.dark)) },
                onClick = { onFlagsChange(LightDarkType.DARK) },
                leadingIcon = {
                    RadioButton(
                        modifier = Modifier.size(24.dp),
                        selected = typeFlags.isDark(),
                        onClick = { onFlagsChange(LightDarkType.DARK) },
                    )
                },
            )
            DropdownMenuItem(
                enabled = typeFlags.isDark(),
                text = {
                    Text(text = stringResource(R.string.extra_dim))
                },
                onClick = {
                    onFlagsChange(
                        if (typeFlags.isExtraDim()) {
                            LightDarkType.DARK
                        } else {
                            LightDarkType.DARK or LightDarkType.EXTRA_DIM
                        },
                    )
                },
                leadingIcon = {
                    Checkbox(
                        modifier = Modifier.size(24.dp),
                        enabled = typeFlags.isDark(),
                        checked = typeFlags.isExtraDim(),
                        onCheckedChange = {
                            onFlagsChange(
                                if (it) {
                                    LightDarkType.DARK or LightDarkType.EXTRA_DIM
                                } else {
                                    LightDarkType.DARK
                                },
                            )
                        },
                    )
                },
            )
        }
    }
}

class LightDarkButtonPPP : CollectionPreviewParameterProvider<Int>(
    listOf(
        LightDarkType.UNSPECIFIED,
        LightDarkType.LIGHT,
        LightDarkType.DARK,
        LightDarkType.DARK or LightDarkType.EXTRA_DIM,
    ),
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewLightDarkButton(
    @PreviewParameter(LightDarkButtonPPP::class) typeFlags: Int,
) {
    WallFlowTheme {
        Surface {
            LightDarkButton(
                typeFlags = typeFlags,
            )
        }
    }
}

@Composable
fun LightDarkInfoDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
) {
    AlertDialog(
        modifier = modifier,
        text = { Text(text = stringResource(R.string.light_dark_info)) },
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
private fun PreviewLightDarkInfoDialog() {
    WallFlowTheme {
        Surface {
            LightDarkInfoDialog()
        }
    }
}
