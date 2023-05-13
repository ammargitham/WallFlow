package com.ammar.havenwalls.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalErrorsRepository @Inject constructor() {
    private val _errors = MutableStateFlow<List<GlobalError>>(emptyList())
    val errors: StateFlow<List<GlobalError>> = _errors

    fun addError(error: GlobalError, replace: Boolean = false) = _errors.update {
        var errorList = it
        if (replace) {
            errorList = errorList.filter { e -> e.type != error.type }
        }
        errorList + error
    }

    fun removeError(error: GlobalError) = _errors.update {
        it.filter { e -> e != error }
    }

    fun removeErrorByType(vararg type: GlobalErrorType) = _errors.update {
        it.filter { e -> e.type !in type }
    }

    enum class GlobalErrorType {
        WALLHAVEN_UNAUTHORISED,
        WALLHAVEN_RATE_LIMIT,
    }

    abstract class GlobalError {
        abstract val type: GlobalErrorType
    }

    class WallHavenUnauthorisedError : GlobalError() {
        override val type: GlobalErrorType = GlobalErrorType.WALLHAVEN_UNAUTHORISED
    }

    class WallHavenRateLimitError : GlobalError() {
        override val type: GlobalErrorType = GlobalErrorType.WALLHAVEN_RATE_LIMIT
    }
}
