package com.ammar.wallflow.ui.common

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap

@Composable
fun ProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float = -1F,
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    if (progress <= -1F) {
        CircularProgressIndicator(
            modifier = modifier,
            strokeCap = strokeCap,
        )
        return
    }
    CircularProgressIndicator(
        modifier = modifier,
        progress = progress,
        strokeCap = strokeCap,
    )
}
