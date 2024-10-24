package com.fleeksoft.ksoup.io

@Deprecated(
    message = "SourceReader.Companion.from(byteArray) is deprecated, use byteArray.inputStream() instead.",
    replaceWith = ReplaceWith("byteArray.inputStream()", "com.fleeksoft.io.inputStream"),
    level = DeprecationLevel.WARNING
)
fun SourceReader.Companion.from(byteArray: ByteArray): SourceReader = SourceReaderByteArray(byteArray)