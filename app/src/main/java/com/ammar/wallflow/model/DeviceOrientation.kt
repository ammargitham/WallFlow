package com.ammar.wallflow.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class DeviceOrientation private constructor(internal val value: Int) {
    companion object {
        val Vertical = DeviceOrientation(0)
        val Horizontal = DeviceOrientation(1)

        fun valueOf(value: Int): DeviceOrientation = if (value == 0) {
            Vertical
        } else {
            Horizontal
        }
    }
}

val MutableDeviceOrientationSaver = Saver<MutableState<DeviceOrientation>, Int>(
    save = { it.value.value },
    restore = { mutableStateOf(DeviceOrientation.valueOf(it)) },
)
