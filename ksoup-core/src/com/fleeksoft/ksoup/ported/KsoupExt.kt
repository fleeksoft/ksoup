package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.KsoupEngineInstance
import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.ported.io.BufferReader
import com.fleeksoft.ksoup.ported.io.Charset
import com.fleeksoft.ksoup.ported.io.Charsets
import com.fleeksoft.ksoup.ported.stream.StreamCharReader

fun String.openBufferReader(charset: Charset? = null): BufferReader =
    KsoupEngineInstance.ksoupEngine.openBufferReader(content = this, charset = charset)

fun ByteArray.openBufferReader(): BufferReader = KsoupEngineInstance.ksoupEngine.openBufferReader(byteArray = this)
fun BufferReader.toStreamCharReader(charset: Charset = Charsets.UTF8, chunkSize: Int = SharedConstants.DefaultBufferSize): StreamCharReader =
    KsoupEngineInstance.ksoupEngine.toStreamCharReader(bufferReader = this, charset = charset, chunkSize = chunkSize)

fun String.toStreamCharReader(charset: Charset = Charsets.UTF8, chunkSize: Int = SharedConstants.DefaultBufferSize): StreamCharReader =
    KsoupEngineInstance.ksoupEngine.toStreamCharReader(content = this, charset = charset, chunkSize = chunkSize)

fun ByteArray.toStreamCharReader(charset: Charset = Charsets.UTF8, chunkSize: Int = SharedConstants.DefaultBufferSize): StreamCharReader =
    KsoupEngineInstance.ksoupEngine.toStreamCharReader(byteArray = this, charset = charset, chunkSize = chunkSize)

fun String.resolveOrNull(access: String): String? = KsoupEngineInstance.ksoupEngine.urlResolveOrNull(base = this, relUrl = access)

fun String.toByteArray(charset: Charset? = null): ByteArray = charset?.toByteArray(this) ?: this.encodeToByteArray()