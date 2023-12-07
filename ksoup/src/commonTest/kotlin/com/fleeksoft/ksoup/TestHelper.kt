package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.ported.BufferReader
import okio.Path
import okio.Path.Companion.toPath

object TestHelper {
    fun getResourceAbsolutePath(resourceName: String): String {
        return "/Users/sabeeh/AndroidStudioProjects/ksoup/ksoup/src/commonTest/resources/$resourceName"
        val rootPath =
            if (Platform.current == PlatformType.IOS) {
                "/Users/runner/work/ksoup/ksoup/ksoup"
            } else {
                "/home/runner/work/ksoup/ksoup/ksoup"
            }
        return "$rootPath/src/commonTest/resources/$resourceName"
//            return "../ksoup/src/commonTest/resources/$resourceName"
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
