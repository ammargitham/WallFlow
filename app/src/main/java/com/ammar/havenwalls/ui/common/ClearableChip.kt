package com.ammar.havenwalls.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ammar.havenwalls.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearableChip(
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    selected: Boolean = false,
    showClearIcon: Boolean = true,
    onClear: () -> Unit = {},
) {
    InputChip(
        modifier = modifier,
        selected = selected,
        onClick = {},
        label = label,
        trailingIcon = if (showClearIcon) {
            {
                Icon(
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(
                            indication = rememberRipple(bounded = false),
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onClear,
                        ),
                    imageVector = Icons.Outlined.Clear,
                    contentDescription = stringResource(R.string.remove),
                )
            }
        } else null,
        interactionSource = NoRippleInteractionSource(),
    )
}
