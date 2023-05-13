package com.ammar.havenwalls.ui.common.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Modified from Accompanist Permission module
 */

@Stable
interface PermissionState {
    val permission: String
    val status: PermissionStatus
    fun launchPermissionRequest()
}

@Composable
fun rememberPermissionState(
    permission: String,
    minimumSdk: Int? = null,
    onPermissionResult: (Boolean) -> Unit = {},
): PermissionState = rememberMutablePermissionState(permission, minimumSdk, onPermissionResult)
