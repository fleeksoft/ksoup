package com.fleeksoft.ksoup

import okio.IOException

class UncheckedIOException : Exception {
    constructor(cause: IOException?) : super(cause)
    constructor(message: String?) : super(IOException(message))

    fun ioException(): Throwable? {
        return cause
    }
}
