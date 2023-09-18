package com.ammar.wallflow.extensions

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.datetime.Instant

fun Instant.format(
    format: String,
): String = SimpleDateFormat(format, Locale.getDefault()).run {
    format(Date(toEpochMilliseconds()))
}
