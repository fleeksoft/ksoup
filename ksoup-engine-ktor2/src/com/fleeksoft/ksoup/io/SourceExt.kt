package com.fleeksoft.ksoup.io

import kotlinx.io.Source
import kotlinx.io.files.Path


@Deprecated(
    message = "SourceReader.Companion.from(source) is deprecated, use Source.asInputStream() instead.",
    level = DeprecationLevel.WARNING
)
fun SourceReader.Companion.from(source: Source): SourceReader = SourceReaderImpl(source)

fun FileSource.Companion.from(file: Path): FileSource = FileSourceImpl(file)
fun FileSource.Companion.from(filePath: String): FileSource = FileSourceImpl(filePath)