package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.io.FileSource
import com.fleeksoft.ksoup.io.FileSourceImpl
import java.io.File

object FileSourceJvmHelper {
    fun jvmFileToFileSource(file: File): FileSource {
        return FileSourceImpl(file.absolutePath)
    }
}