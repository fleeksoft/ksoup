package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.ported.BufferReader
import okio.Path
import okio.Path.Companion.toPath

object TestHelper {
//    some tests ignored for specific platform
    const val forceAllTestsRun = false

    fun getResourceAbsolutePath(resourceName: String): String {
        return "${BuildConfig.PROJECT_ROOT}/ksoup/src/commonTest/resources/$resourceName"
        /*return when (Platform.current) {
            PlatformType.JVM, PlatformType.ANDROID -> {
                "src/commonTest/resources/$resourceName"
            }

            PlatformType.JS -> {
                "../../../../ksoup/src/commonTest/resources/$resourceName"
            }

            PlatformType.IOS -> {
                "/Users/runner/work/ksoup/ksoup/ksoup/src/commonTest/resources/$resourceName"
            }

            else -> "src/commonTest/resources/$resourceName"
        }*/
    }

    fun getFileAsString(file: Path): String {
        val bytes: ByteArray =
            if (file.name.endsWith(".gz")) {
                readGzipFile(file).readByteArray()
            } else {
                readFile(file).readByteArray()
            }
        return bytes.decodeToString()
    }

    fun resourceFilePathToBufferReader(path: String): BufferReader {
        val file = this.getResourceAbsolutePath(path)
        return pathToBufferReader(file.toPath())
    }

    fun pathToBufferReader(file: Path): BufferReader {
        return if (file.name.endsWith(".gz")) {
            BufferReader(readGzipFile(file))
        } else {
            BufferReader(readFile(file))
        }
    }
}
