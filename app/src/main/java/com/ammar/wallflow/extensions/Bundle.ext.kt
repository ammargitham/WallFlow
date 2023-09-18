package com.ammar.wallflow.extensions

import android.os.Build
import android.os.Bundle

fun <T> Bundle.getParcelableCompat(
    name: String,
    clazz: Class<T>,
) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    this.getParcelable(name, clazz)
} else {
    @Suppress("DEPRECATION")
    this.getParcelable(name) as T?
}
