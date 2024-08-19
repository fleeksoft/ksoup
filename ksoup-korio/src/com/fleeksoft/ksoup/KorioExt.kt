package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.internal.Normalizer
import com.fleeksoft.ksoup.ported.io.SourceReader
import com.fleeksoft.ksoup.ported.openSourceReader
import korlibs.io.file.VfsFile
import korlibs.io.file.fullName

suspend fun VfsFile.openStream(): SourceReader {
    val name = Normalizer.lowerCase(this.fullName)
    if (name.endsWith(".gz") || name.endsWith(".z")) {
        val byteArray = this.readChunk(0, 2)
        val zipped =
            (byteArray.size == 2 && byteArray[0].toInt() == 31 && byteArray[1].toInt() == -117) // gzip magic bytes 31(0x1f), -117(0x1f)
        if (zipped) {
            return readGzipFile(this)
        }
    }
    return this.readAll().openSourceReader()
}