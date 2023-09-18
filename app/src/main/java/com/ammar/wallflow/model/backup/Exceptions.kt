package com.ammar.wallflow.model.backup

abstract class RestoreException : Exception {
    constructor() : super()
    constructor(t: Throwable) : super(t)
    constructor(message: String) : super(message)
}

class FileNotFoundException(t: Throwable) : RestoreException(t)

class InvalidJsonException : RestoreException {
    constructor() : super()
    constructor(t: Throwable) : super(t)
    constructor(message: String) : super(message)
}
