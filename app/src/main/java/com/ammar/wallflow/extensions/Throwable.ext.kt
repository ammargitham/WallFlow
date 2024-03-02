package com.ammar.wallflow.extensions

val Throwable.rootCause: Throwable
    get() {
        var rootCause: Throwable = this
        while (rootCause.cause !== rootCause) {
            rootCause = rootCause.cause ?: break
        }
        return rootCause
    }
