package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.KsoupEngineInstance
import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.io.Charset
import com.fleeksoft.ksoup.io.FileSource
import com.fleeksoft.ksoup.io.SourceReader
import com.fleeksoft.ksoup.io.from
import com.fleeksoft.ksoup.ported.io.BufferedReader
import com.fleeksoft.ksoup.ported.io.Charsets
import com.fleeksoft.ksoup.ported.io.InputSourceReader
import com.fleeksoft.ksoup.ported.io.Reader

fun String.openSourceReader(charset: Charset? = null): SourceReader = SourceReader.from(charset?.toByteArray(this) ?: this.encodeToByteArray())

fun ByteArray.openSourceReader(): SourceReader = SourceReader.from(this)
fun SourceReader.toReader(charset: Charset = Charsets.UTF8, chunkSize: Int = SharedConstants.DefaultBufferSize): Reader =
    BufferedReader(InputSourceReader(this, charset = charset), chunkSize)

fun String.toByteArray(charset: Charset? = null): ByteArray = charset?.toByteArray(this) ?: this.encodeToByteArray()

fun String.toSourceFile(): FileSource = KsoupEngineInstance.ksoupEngine.pathToFileSource(this)