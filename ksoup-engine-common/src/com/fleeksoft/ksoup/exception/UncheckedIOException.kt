package com.fleeksoft.ksoup.exception

public class UncheckedIOException : Exception {
    public constructor(cause: IOException) : super(cause)
    public constructor(message: String) : super(IOException(message))

    public fun ioException(): Throwable? {
        return cause
    }
}
