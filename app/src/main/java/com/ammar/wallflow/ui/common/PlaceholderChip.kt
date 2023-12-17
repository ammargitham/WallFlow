package com.ammar.wallflow.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.fade
import com.google.accompanist.placeholder.material3.placeholder

@Composable
fun PlaceholderChip(
    modifier: Modifier = Modifier,
    shape: Shape = AssistChipDefaults.shape,
    height: Dp = AssistChipDefaults.Height,
    color: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
) {
    Box(
        modifier = modifier
            .height(height)
            .fillMaxWidth()
            .placeholder(
                color = color,
                visible = true,
                highlight = PlaceholderHighlight.fade(),
                shape = shape,
            ),
    )
}
