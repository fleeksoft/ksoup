package com.fleeksoft.ksoup.ported.io

import korlibs.io.file.VfsFile
import korlibs.io.lang.toByteArray

fun String.openBufferReader(charset: Charset = Charsets.UTF8) = BufferReaderImpl(this.toByteArray(charset))
fun ByteArray.openBufferReader() = BufferReaderImpl(this)

//fixme: reading all bytes
suspend fun VfsFile.openBufferReader(): BufferReader = this.readAll().openBufferReader()