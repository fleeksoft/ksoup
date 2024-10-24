package com.fleeksoft.ksoup.io

import java.io.File
import java.io.InputStream

// TODO: for jvm we can use streaming api in lite module

@Deprecated(
    message = "SourceReader.Companion.from(InputStream) is deprecated, use com.fleeksoft.InputStream instead.",
    level = DeprecationLevel.WARNING
)
fun SourceReader.Companion.from(inputStream: InputStream): SourceReader = SourceReader.from(inputStream.readAllBytes())
fun FileSource.Companion.from(file: File): FileSource = TODO("File Source not supported in lite")
fun FileSource.Companion.from(file: String): FileSource = TODO("File Source not supported in lite")