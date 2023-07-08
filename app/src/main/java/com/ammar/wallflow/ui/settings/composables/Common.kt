package com.ammar.wallflow.ui.settings.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ammar.wallflow.R
import org.tensorflow.lite.task.core.ComputeSettings

@Composable
internal fun delegateString(delegate: ComputeSettings.Delegate) = stringResource(
    when (delegate) {
        ComputeSettings.Delegate.NONE -> R.string.cpu
        ComputeSettings.Delegate.NNAPI -> R.string.nnapi
        ComputeSettings.Delegate.GPU -> R.string.gpu
    }
)
