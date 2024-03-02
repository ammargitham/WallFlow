package com.ammar.wallflow.ui.common

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.util.PatternsCompat
import com.ammar.wallflow.R
import com.ammar.wallflow.extensions.isValidFileName

class NameState(
    val context: Context,
    val name: String? = null,
    private val nameExists: State<Boolean> = mutableStateOf(false),
) :
    TextFieldState(
        validator = { it.isNotBlank() && !nameExists.value },
        errorFor = {
            context.getString(
                if (it.isBlank()) R.string.name_cannot_be_empty else R.string.name_already_used,
            )
        },
    ) {
    init {
        name?.let { text = it }
    }
}

fun nameStateSaver(context: Context) = textFieldStateSaver(NameState(context))

class UrlState(
    val context: Context,
    val url: String? = null,
) :
    TextFieldState(
        validator = { it.isNotBlank() && PatternsCompat.WEB_URL.matcher(it).matches() },
        errorFor = {
            context.getString(
                if (it.isBlank()) R.string.url_cannot_be_empty else R.string.invalid_url,
            )
        },
    ) {
    init {
        url?.let { text = it }
    }
}

fun urlStateSaver(context: Context) = textFieldStateSaver(UrlState(context))

class IntState(
    val value: Int? = null,
    private val allowNegative: Boolean = false,
    errorFor: (String) -> String = { "" },
) :
    TextFieldState(
        validator = {
            val valInt = it.toIntOrNull()
            if (valInt == null) {
                false
            } else {
                allowNegative || valInt >= 0
            }
        },
        errorFor = { errorFor(it) },
    ) {
    init {
        value?.let { text = it.toString() }
    }
}

fun intStateSaver(errorFor: (String) -> String) = textFieldStateSaver(IntState(errorFor = errorFor))

class FileNameState(
    val context: Context,
    val fileName: String? = null,
    private val fileNameExists: State<Boolean> = mutableStateOf(false),
) :
    TextFieldState(
        validator = { it.isNotBlank() && !fileNameExists.value && it.isValidFileName() },
        errorFor = {
            context.getString(
                when {
                    it.isBlank() -> R.string.file_name_cannot_be_empty
                    fileNameExists.value -> R.string.file_name_already_used
                    !it.isValidFileName() -> R.string.file_name_invalid
                    else -> R.string.file_name_invalid
                },
            )
        },
    ) {
    init {
        fileName?.let { text = it }
    }
}

fun fileNameStateSaver(context: Context) = textFieldStateSaver(FileNameState(context))
