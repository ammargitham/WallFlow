package com.ammar.wallflow.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.extensions.blend
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
fun ClearableChip(
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    selected: Boolean = false,
    showClearIcon: Boolean = true,
    colors: SelectableChipColors = InputChipDefaults.inputChipColors(),
    errorColors: SelectableChipColors = InputChipDefaults.inputChipColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        selectedContainerColor = MaterialTheme.colorScheme.errorContainer.blend(
            color = Color.Red,
            ratio = 0.2f,
        ),
        labelColor = MaterialTheme.colorScheme.error,
        selectedLabelColor = MaterialTheme.colorScheme.error,
        leadingIconColor = MaterialTheme.colorScheme.error,
        selectedLeadingIconColor = MaterialTheme.colorScheme.error,
        trailingIconColor = MaterialTheme.colorScheme.error,
        selectedTrailingIconColor = MaterialTheme.colorScheme.error,
    ),
    border: BorderStroke = InputChipDefaults.inputChipBorder(
        enabled = true,
        selected = false,
    ),
    errorBorder: BorderStroke = InputChipDefaults.inputChipBorder(
        enabled = true,
        selected = false,
        borderColor = MaterialTheme.colorScheme.error,
        selectedBorderColor = MaterialTheme.colorScheme.error,
        selectedBorderWidth = 1.dp,
    ),
    isError: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    onClear: () -> Unit = {},
) {
    InputChip(
        modifier = modifier,
        selected = selected,
        colors = if (isError) errorColors else colors,
        border = if (isError) errorBorder else border,
        onClick = {},
        label = label,
        trailingIcon = if (showClearIcon) {
            {
                Icon(
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(
                            indication = ripple(bounded = false),
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onClear,
                        ),
                    imageVector = Icons.Outlined.Clear,
                    contentDescription = stringResource(R.string.remove),
                )
            }
        } else {
            null
        },
        interactionSource = NoRippleInteractionSource(),
        leadingIcon = leadingIcon,
    )
}

private data class ClearableChipParams(
    val label: String,
    val isError: Boolean,
    val selected: Boolean,
)

private class ClearableChipParamsProvider :
    CollectionPreviewParameterProvider<ClearableChipParams>(
        listOf(
            ClearableChipParams(
                label = "test",
                isError = false,
                selected = false,
            ),
            ClearableChipParams(
                label = "test1",
                isError = true,
                selected = false,
            ),
            ClearableChipParams(
                label = "test2",
                isError = true,
                selected = true,
            ),
        ),
    )

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewClearableChip(
    @PreviewParameter(ClearableChipParamsProvider::class) params: ClearableChipParams,
) {
    WallFlowTheme {
        Surface {
            Box(
                modifier = Modifier.padding(8.dp),
            ) {
                ClearableChip(
                    label = { Text(text = params.label) },
                    isError = params.isError,
                    selected = params.selected,
                )
            }
        }
    }
}
