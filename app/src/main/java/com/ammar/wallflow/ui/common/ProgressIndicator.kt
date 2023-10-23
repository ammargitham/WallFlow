package com.ammar.wallflow.ui.common

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap

@Composable
fun ProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float = -1F,
    strokeCap: StrokeCap = StrokeCap.Round,
    circular: Boolean = true,
) {
    if (progress <= -1F) {
        if (circular) {
            CircularProgressIndicator(
                modifier = modifier,
                strokeCap = strokeCap,
            )
        } else {
            LinearProgressIndicator(
                modifier = modifier,
                strokeCap = strokeCap,
            )
        }
    } else {
        if (circular) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = modifier,
                strokeCap = strokeCap,
            )
        } else {
            LinearProgressIndicator(
                progress = { progress },
                modifier = modifier,
                strokeCap = strokeCap,
            )
        }
    }
}
