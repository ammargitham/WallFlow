package com.ammar.havenwalls.extensions

import android.content.Intent
import android.os.Build

fun <T> Intent.getParcelExtra(
    name: String,
    clazz: Class<T>,
) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    this.getParcelableExtra(name, clazz)
} else {
    @Suppress("DEPRECATION")
    this.getParcelableExtra(name) as T?
}
