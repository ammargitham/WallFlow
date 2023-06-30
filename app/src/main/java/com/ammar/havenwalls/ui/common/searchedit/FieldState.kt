package com.ammar.havenwalls.ui.common.searchedit

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.ammar.havenwalls.R
import com.ammar.havenwalls.ui.common.TextFieldState
import com.ammar.havenwalls.ui.common.textFieldStateSaver

class NameState(
    val context: Context,
    val name: String? = null,
    private val nameExists: State<Boolean> = mutableStateOf(false),
) :
    TextFieldState(
        validator = { it.isNotBlank() && !nameExists.value },
        errorFor = {
            context.getString(
                if (it.isBlank()) R.string.name_cannot_be_empty else R.string.name_already_used
            )
        }
    ) {
    init {
        name?.let { text = it }
    }
}

fun nameStateSaver(context: Context) = textFieldStateSaver(NameState(context))

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
        errorFor = { errorFor(it) }
    ) {
    init {
        value?.let { text = it.toString() }
    }
}

fun intStateSaver(errorFor: (String) -> String) = textFieldStateSaver(IntState(errorFor = errorFor))
