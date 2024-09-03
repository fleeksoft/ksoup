package com.fleeksoft.ksoup.io

import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import java.io.File
import java.io.InputStream


fun FileSource.Companion.from(file: File): FileSource = FileSource.from(Path(file.absolutePath))
fun SourceReader.Companion.from(inputStream: InputStream): SourceReader = SourceReader.from(inputStream.asSource().buffered())