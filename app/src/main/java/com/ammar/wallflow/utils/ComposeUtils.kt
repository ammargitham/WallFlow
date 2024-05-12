package com.ammar.wallflow.utils

import com.ammar.wallflow.DISABLED_ALPHA

fun getAlpha(enabled: Boolean) = if (enabled) 1f else DISABLED_ALPHA
