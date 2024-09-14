package com.fleeksoft.ksoup.io

fun SourceReader.Companion.from(byteArray: ByteArray): SourceReader = SourceReaderByteArray(byteArray)