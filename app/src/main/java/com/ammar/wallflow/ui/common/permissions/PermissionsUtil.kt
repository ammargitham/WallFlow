package com.ammar.wallflow.ui.common.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ammar.wallflow.extensions.wallpaperManager
import com.ammar.wallflow.utils.StoragePermissions

@Stable
sealed interface PermissionStatus {
    data object Granted : PermissionStatus
    data class Denied(
        val shouldShowRationale: Boolean,
    ) : PermissionStatus
}

/**
 * `true` if the permission is granted.
 */
val PermissionStatus.isGranted: Boolean
    get() = this == PermissionStatus.Granted

/**
 * `true` if a rationale should be presented to the user.
 */
val PermissionStatus.shouldShowRationale: Boolean
    get() = when (this) {
        PermissionStatus.Granted -> false
        is PermissionStatus.Denied -> shouldShowRationale
    }

/**
 * Effect that updates the `hasPermission` state of a revoked [MutablePermissionState] permission
 * when the lifecycle gets called with [lifecycleEvent].
 */
@Composable
internal fun PermissionLifecycleCheckerEffect(
    permissionState: MutablePermissionState,
    lifecycleEvent: Lifecycle.Event = Lifecycle.Event.ON_RESUME,
) {
    // Check if the permission was granted when the lifecycle is resumed.
    // The user might've gone to the Settings screen and granted the permission.
    val permissionCheckerObserver = remember(permissionState) {
        LifecycleEventObserver { _, event ->
            if (event == lifecycleEvent) {
                // If the permission is revoked, check again.
                // We don't check if the permission was denied as that triggers a process restart.
                if (permissionState.status != PermissionStatus.Granted) {
                    permissionState.refreshPermissionStatus()
                }
            }
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, permissionCheckerObserver) {
        lifecycle.addObserver(permissionCheckerObserver)
        onDispose { lifecycle.removeObserver(permissionCheckerObserver) }
    }
}

/**
 * Effect that updates the `hasPermission` state of a list of permissions
 * when the lifecycle gets called with [lifecycleEvent] and the permission is revoked.
 */
@Composable
internal fun PermissionsLifecycleCheckerEffect(
    permissions: List<MutablePermissionState>,
    lifecycleEvent: Lifecycle.Event = Lifecycle.Event.ON_RESUME,
) {
    // Check if the permission was granted when the lifecycle is resumed.
    // The user might've gone to the Settings screen and granted the permission.
    val permissionsCheckerObserver = remember(permissions) {
        LifecycleEventObserver { _, event ->
            if (event == lifecycleEvent) {
                for (permission in permissions) {
                    // If the permission is revoked, check again. We don't check if the permission
                    // was denied as that triggers a process restart.
                    if (permission.status != PermissionStatus.Granted) {
                        permission.refreshPermissionStatus()
                    }
                }
            }
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, permissionsCheckerObserver) {
        lifecycle.addObserver(permissionsCheckerObserver)
        onDispose { lifecycle.removeObserver(permissionsCheckerObserver) }
    }
}

internal fun Context.checkPermission(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

internal fun Activity.shouldShowRationale(permission: String) =
    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

internal fun Context.checkNotificationPermission() = NotificationManagerCompat.from(this)
    .areNotificationsEnabled()

internal fun Context.checkSetWallpaperPermission(): Boolean {
    val wallpaperManager = wallpaperManager
    return wallpaperManager.isWallpaperSupported && wallpaperManager.isSetWallpaperAllowed
}

@SuppressLint("InlinedApi")
@Composable
fun rememberDownloadPermissionsState(
    onShowRationale: () -> Unit = {},
    onGranted: () -> Unit = {},
): MultiplePermissionsState {
    val storagePerms = remember {
        StoragePermissions.getPermissions(
            action = StoragePermissions.Action.READ_AND_WRITE,
            types = listOf(StoragePermissions.FileType.Image),
            createdBy = StoragePermissions.CreatedBy.Self,
        ).map { MultiplePermissionItem(permission = it) }
    }

    return rememberMultiplePermissionsState(
        permissions = storagePerms + MultiplePermissionItem(
            permission = Manifest.permission.POST_NOTIFICATIONS,
            minimumSdk = Build.VERSION_CODES.TIRAMISU,
        ),
    ) { permissionStates ->
        val shouldShowRationale = permissionStates.map { it.status.shouldShowRationale }.any { it }
        if (shouldShowRationale) {
            onShowRationale()
            return@rememberMultiplePermissionsState
        }
        // check if storage permissions are granted (notification permission is optional)
        val storagePermStrings = storagePerms.map { it.permission }
        val allGranted = permissionStates
            .filter { it.permission in storagePermStrings }
            .all { it.status.isGranted }
        if (!allGranted) return@rememberMultiplePermissionsState
        onGranted()
    }
}
