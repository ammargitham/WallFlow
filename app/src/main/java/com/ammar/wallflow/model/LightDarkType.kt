package com.ammar.wallflow.model

import androidx.compose.runtime.Stable

object LightDarkType {
    const val UNSPECIFIED = 0
    const val LIGHT = 1
    const val DARK = 2
    const val EXTRA_DIM = 4
}

@Stable
fun Int.isLight(): Boolean = this and LightDarkType.LIGHT == LightDarkType.LIGHT

@Stable
fun Int.isDark(): Boolean = this and LightDarkType.DARK == LightDarkType.DARK

@Stable
fun Int.isUnspecified(): Boolean = this == LightDarkType.UNSPECIFIED

@Stable
fun Int.isExtraDim(): Boolean = this and LightDarkType.EXTRA_DIM == LightDarkType.EXTRA_DIM
