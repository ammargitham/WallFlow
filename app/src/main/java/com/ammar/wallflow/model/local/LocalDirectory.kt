package com.ammar.wallflow.model.local

import android.net.Uri
import androidx.compose.runtime.Stable

@Stable
data class LocalDirectory(
    val uri: Uri,
    val path: String,
)
