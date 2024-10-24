package com.fleeksoft.ksoup

import com.fleeksoft.io.InputStream
import com.fleeksoft.io.inputStream
import korlibs.io.compression.deflate.GZIP
import korlibs.io.compression.uncompress
import korlibs.io.file.VfsFile
import korlibs.io.file.fullName
import korlibs.io.file.readAsSyncStream
import korlibs.io.stream.readAll

suspend fun VfsFile.inputStream(): InputStream {
    val name = this.fullName.lowercase()
    if (name.endsWith(".gz") || name.endsWith(".z")) {
        val byteArray = this.readChunk(0, 2)
        val zipped =
            (byteArray.size == 2 && byteArray[0].toInt() == 31 && byteArray[1].toInt() == -117) // gzip magic bytes 31(0x1f), -117(0x1f)
        if (zipped) {
            return this.readAsSyncStream().readAll().uncompress(GZIP).inputStream()
        }
    }
    return this.readAll().inputStream()
}