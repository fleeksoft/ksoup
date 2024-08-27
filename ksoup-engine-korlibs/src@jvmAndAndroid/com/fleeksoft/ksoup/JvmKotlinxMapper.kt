package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.io.*
import korlibs.io.file.std.toVfs
import korlibs.io.stream.SyncInputStream
import korlibs.io.stream.toAsync
import korlibs.io.stream.toAsyncStream
import korlibs.io.stream.toSyncInputStream
import java.io.File
import java.io.InputStream

object JvmKotlinxMapper {
    fun jvmFileToFileSource(file: File): FileSource {
        return FileSourceImpl(file.toVfs())
    }

    fun jvmInputStreamToSourceReader(inputStream: InputStream): SourceReader {
        return SourceReaderImpl(inputStream.readAllBytes())
    }
}