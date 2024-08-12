package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.ported.io.*
import com.fleeksoft.ksoup.ported.stream.StreamCharReader

// placeholders from other modules

fun String.openBufferReader(charset: Charset? = null): BufferReader = TODO()

fun ByteArray.openBufferReader(): BufferReader = TODO()
fun BufferReader.toStreamCharReader(charset: Charset = Charsets.UTF8, chunkSize: Int = SharedConstants.DefaultBufferSize): StreamCharReader = TODO()

fun String.toStreamCharReader(charset: Charset = Charsets.UTF8, chunkSize: Int = SharedConstants.DefaultBufferSize): StreamCharReader = TODO()

fun ByteArray.toStreamCharReader(charset: Charset = Charsets.UTF8, chunkSize: Int = SharedConstants.DefaultBufferSize): StreamCharReader = TODO()