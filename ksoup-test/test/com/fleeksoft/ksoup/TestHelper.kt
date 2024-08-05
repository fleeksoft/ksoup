package com.fleeksoft.ksoup

import korlibs.io.file.VfsFile
import korlibs.io.file.fullName
import korlibs.io.file.std.uniVfs
import korlibs.io.stream.SyncStream
import korlibs.io.stream.readAll

object TestHelper {

    suspend fun readGzipResource(file: String): SyncStream {
        return readGzipFile(getResourceAbsolutePath(file))
    }

    fun getResourceAbsolutePath(resourceName: String): String {
        if (Platform.current == PlatformType.WASM_JS) {
            return resourceName
        }
        return "${BuildConfig.PROJECT_ROOT}/ksoup-test/testResources/$resourceName"
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
        val file = this.getResourceAbsolutePath(path).uniVfs
        return pathToStream(file)
    }

    suspend fun pathToStream(file: VfsFile): SyncStream {
        return if (file.fullName.endsWith(".gz")) {
            readGzipFile(file.absolutePath)
        } else {
            readFile(file.absolutePath)
        }
    }
}
