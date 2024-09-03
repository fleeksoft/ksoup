package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.io.FileSource
import com.fleeksoft.ksoup.io.SourceReader
import com.fleeksoft.ksoup.io.from
import java.io.File
import java.io.InputStream
import kotlin.io.path.absolutePathString

fun File.toFileSource(): FileSource = FileSource.from(this)
fun java.nio.file.Path.toFileSource(): FileSource = FileSource.from(this.absolutePathString())
fun InputStream.toSourceReader(): SourceReader = SourceReader.from(this)