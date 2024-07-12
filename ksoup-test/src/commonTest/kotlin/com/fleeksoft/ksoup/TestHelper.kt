package com.fleeksoft.ksoup

import korlibs.io.file.VfsFile
import korlibs.io.file.fullName
import korlibs.io.file.std.uniVfs
import korlibs.io.stream.SyncStream
import korlibs.io.stream.readAll

object TestHelper {

    fun getResourceAbsolutePath(resourceName: String): String {
        if (Platform.current == PlatformType.WASMJS) {
            return resourceName
        }
        return "${BuildConfig.PROJECT_ROOT}/ksoup-test/src/commonTest/resources/$resourceName"
    }

    suspend fun getFileAsString(file: VfsFile): String {
        val bytes: ByteArray =
            if (file.fullName.endsWith(".gz")) {
                readGzipFile(file.absolutePath).readAll()
            } else {
                readFile(file.absolutePath).readAll()
            }
        return bytes.decodeToString()
    }

    suspend fun resourceFilePathToStream(path: String): SyncStream {
        val file = this.getResourceAbsolutePath(path)
        return pathToStream(file.uniVfs)
    }

    suspend fun pathToStream(file: VfsFile): SyncStream {
        return if (file.fullName.endsWith(".gz")) {
            readGzipFile(file.absolutePath)
        } else {
            readFile(file.absolutePath)
        }
    }
}
