package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.KsoupEngineInstance
import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.ported.io.*

fun String.openSourceReader(charset: Charset? = null): SourceReader =
    KsoupEngineInstance.ksoupEngine.openSourceReader(content = this, charset = charset)

fun ByteArray.openSourceReader(): SourceReader = KsoupEngineInstance.ksoupEngine.openSourceReader(byteArray = this)
fun SourceReader.toReader(charset: Charset = Charsets.UTF8, chunkSize: Int = SharedConstants.DefaultBufferSize): Reader =
    BufferedReader(InputSourceReader(this, charset = charset), chunkSize)

fun String.toReader(): StringReader = StringReader(this)

fun String.resolveOrNull(access: String): String? = KsoupEngineInstance.ksoupEngine.urlResolveOrNull(base = this, relUrl = access)

fun String.toByteArray(charset: Charset? = null): ByteArray = charset?.toByteArray(this) ?: this.encodeToByteArray()