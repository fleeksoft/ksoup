package com.fleeksoft.ksoup.io

import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.File
import java.io.InputStream


fun FileSource.Companion.from(file: File): FileSource = FileSource.from(file)
fun SourceReader.Companion.from(inputStream: InputStream): SourceReader = SourceReader.from(inputStream.asSource().buffered())