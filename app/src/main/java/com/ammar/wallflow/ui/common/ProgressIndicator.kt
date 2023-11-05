package com.ammar.wallflow.ui.common

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

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
                modifier = modifier.testTag("circular-progress"),
                strokeCap = strokeCap,
            )
        } else {
            LinearProgressIndicator(
                modifier = modifier.testTag("linear-progress"),
                strokeCap = strokeCap,
            )
        }
    } else {
        if (circular) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = modifier
                    .testTag("circular-progress")
                    .semantics { contentDescription = "Progress $progress" },
                strokeCap = strokeCap,
            )
        } else {
            LinearProgressIndicator(
                progress = { progress },
                modifier = modifier
                    .testTag("linear-progress")
                    .semantics { contentDescription = "Progress $progress" },
                strokeCap = strokeCap,
            )
        }
    }
}
