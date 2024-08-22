package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.io.FileSource
import com.fleeksoft.ksoup.io.FileSourceImpl
import korlibs.io.file.std.toVfs
import java.io.File

object FileSourceJvmHelper {
    fun jvmFileToFileSource(file: File): FileSource {
        return FileSourceImpl(file.toVfs())
    }
}