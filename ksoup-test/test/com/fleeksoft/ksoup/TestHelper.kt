package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.io.SourceReader
import com.fleeksoft.ksoup.ported.openSourceReader
import korlibs.io.compression.deflate.GZIP
import korlibs.io.compression.uncompress
import korlibs.io.file.VfsFile
import korlibs.io.file.fullName
import korlibs.io.file.readAsSyncStream
import korlibs.io.file.std.uniVfs
import korlibs.io.stream.readAll

object TestHelper {

    suspend fun readGzipResource(file: String): SourceReader {
        return readGzipFile(getResourceAbsolutePath(file).uniVfs)
    }

    suspend fun readResource(file: String): SourceReader {
        if (file.endsWith(".gz") || file.endsWith(".z")) {
            return readGzipResource(file)
        }
        return readFile(getResourceAbsolutePath(file).uniVfs)
    }

    fun getResourceAbsolutePath(resourceName: String): String {
        if (Platform.isWindows()) {
            return "../../../../testResources/$resourceName"
        } else if (Platform.isJsOrWasm()) {
            return "https://raw.githubusercontent.com/fleeksoft/ksoup/release/ksoup-test/testResources/$resourceName"
        }
        return "${BuildConfig.PROJECT_ROOT}/ksoup-test/testResources/$resourceName"
    }

    suspend fun getFileAsString(file: VfsFile): String {
        val bytes: ByteArray = if (file.fullName.endsWith(".gz")) {
            readGzipFile(file).readAllBytes()
        } else {
            readFile(file).readAllBytes()
        }
        return bytes.decodeToString()
    }

    suspend fun resourceFilePathToStream(path: String): SourceReader {
        val file = this.getResourceAbsolutePath(path).uniVfs
        return pathToStream(file)
    }

    suspend fun pathToStream(file: VfsFile): SourceReader {
        return if (file.fullName.endsWith(".gz") || file.fullName.endsWith(".z")) {
            readGzipFile(file)
        } else {
            readFile(file)
        }
    }

    suspend fun readFile(file: VfsFile): SourceReader {
        return file.readAll().openSourceReader()
    }

    suspend fun readGzipFile(file: VfsFile): SourceReader {
        return file.readAsSyncStream().readAll().uncompress(GZIP).openSourceReader()
    }
}
