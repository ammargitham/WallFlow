package com.ammar.havenwalls.model

data class MenuItem(
    val text: String,
    val value: String,
    val onClick: (() -> Unit)? = null,
    val enabled: Boolean = true,
)
