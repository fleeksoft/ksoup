package com.fleeksoft.ksoup.kotlinx

import com.fleeksoft.ksoup.internal.Normalizer
import com.fleeksoft.ksoup.ported.io.BufferReader
import com.fleeksoft.ksoup.ported.openBufferReader
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

suspend fun Path.openStream(): BufferReader {
    val name = Normalizer.lowerCase(this.name)
    if (name.endsWith(".gz") || name.endsWith(".z")) {
        val source = SystemFileSystem.source(this)
        val buffer = Buffer()
        source.readAtMostTo(buffer, 2)
        val zipped =
            (buffer.size == 2L && buffer.readByte().toInt() == 31 && buffer.readByte().toInt() == -117) // gzip magic bytes 31(0x1f), -117(0x1f)
        if (zipped) {
            return readGzipFile(this)
        }
    }
    return SystemFileSystem.source(this).buffered().readByteArray().openBufferReader()
}


suspend fun readGzipFile(path: Path): BufferReader {
    TODO("Gzip not supported")
}