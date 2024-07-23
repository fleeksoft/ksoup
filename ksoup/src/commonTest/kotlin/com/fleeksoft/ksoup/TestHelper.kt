package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.ported.BufferReader
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.readByteArray

object TestHelper {
    //    some tests ignored for specific platform
    const val forceAllTestsRun = false

    fun getResourceAbsolutePath(resourceName: String): String {
        return "${BuildConfig.PROJECT_ROOT}/ksoup/src/commonTest/resources/$resourceName"
    }

    fun getFileAsString(file: Path): String {
        val bytes: ByteArray =
            if (file.name.endsWith(".gz")) {
                readGzipFile(file).buffered().readByteArray()
            } else {
                readFile(file).buffered().readByteArray()
            }
        return bytes.decodeToString()
    }

    fun resourceFilePathToBufferReader(path: String): BufferReader {
        val file = this.getResourceAbsolutePath(path)
        return pathToBufferReader(Path(file))
    }

    fun pathToBufferReader(file: Path): BufferReader {
        return if (file.name.endsWith(".gz")) {
            BufferReader(readGzipFile(file))
        } else {
            BufferReader(readFile(file))
        }
    }
}
