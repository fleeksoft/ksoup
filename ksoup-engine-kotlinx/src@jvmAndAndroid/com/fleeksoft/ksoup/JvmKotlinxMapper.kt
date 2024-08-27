package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.io.FileSource
import com.fleeksoft.ksoup.io.FileSourceImpl
import com.fleeksoft.ksoup.io.SourceReader
import com.fleeksoft.ksoup.io.SourceReaderImpl
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.File
import java.io.InputStream

object JvmKotlinxMapper {
    fun jvmFileToFileSource(file: File): FileSource {
        return FileSourceImpl(file.absolutePath)
    }
    fun jvmInputStreamToSourceReader(inputStream: InputStream): SourceReader {
        return SourceReaderImpl(inputStream.asSource().buffered())
    }
}