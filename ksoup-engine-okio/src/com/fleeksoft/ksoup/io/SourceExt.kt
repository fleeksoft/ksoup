package com.fleeksoft.ksoup.io

import com.fleeksoft.io.InputStream
import okio.Path
import okio.Source
import okio.buffer


@Deprecated(
    message = "SourceReader.Companion.from(source) is deprecated, use Source.asInputStream() instead.",
    level = DeprecationLevel.WARNING
)
fun SourceReader.Companion.from(source: Source): SourceReader = SourceReaderImpl(source)

fun FileSource.Companion.from(file: Path): FileSource = FileSourceImpl(file)
fun FileSource.Companion.from(filePath: String): FileSource = FileSourceImpl(filePath)

fun Source.asInputStream(): InputStream = OkioSourceInputStream(this.buffer())