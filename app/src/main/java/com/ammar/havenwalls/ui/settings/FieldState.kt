package com.ammar.havenwalls.ui.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.util.PatternsCompat
import com.ammar.havenwalls.ui.common.TextFieldState
import com.ammar.havenwalls.ui.common.textFieldStateSaver

class NameState(
    val name: String? = null,
    private val nameExists: State<Boolean> = mutableStateOf(false),
) :
    TextFieldState(
        validator = { it.isNotBlank() && !nameExists.value },
        errorFor = { if (it.isBlank()) "Name cannot be empty" else "Name already used" }
    ) {
    init {
        name?.let { text = it }
    }
}

val NameStateSaver = textFieldStateSaver(NameState())

class UrlState(val url: String? = null) :
    TextFieldState(
        validator = { it.isNotBlank() && PatternsCompat.WEB_URL.matcher(it).matches() },
        errorFor = { if (it.isBlank()) "URL cannot be empty" else "Invalid URL" }
    ) {
    init {
        url?.let { text = it }
    }
}

val UrlStateSaver = textFieldStateSaver(UrlState())
