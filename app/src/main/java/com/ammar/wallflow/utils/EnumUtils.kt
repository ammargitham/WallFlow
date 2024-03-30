package com.ammar.wallflow.utils

inline fun <reified T : Enum<T>> valueOf(type: String?) = if (type == null) {
    null
} else {
    try {
        java.lang.Enum.valueOf(T::class.java, type)
    } catch (e: IllegalArgumentException) {
        null
    }
}
