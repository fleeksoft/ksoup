package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.io.SourceReader
import com.fleeksoft.ksoup.ported.io.Charsets
import com.fleeksoft.ksoup.ported.openSourceReader
import com.fleeksoft.ksoup.ported.toByteArray
import korlibs.io.compression.deflate.GZIP
import korlibs.io.compression.uncompress
import korlibs.io.file.std.uniVfs
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

object TestHelper {

    suspend fun readGzipResource(resource: String): SourceReader {
        return readGzipFile(resource)
    }

    suspend fun readResource(resource: String): SourceReader {
        if (resource.endsWith(".gz") || resource.endsWith(".z")) {
            return readGzipResource(resource)
        }
        return readFile(resource)
    }

    fun getResourceAbsolutePath(resourceName: String, absForWindows: Boolean = true): String {
        if (Platform.isWindows() && BuildConfig.isKorlibs && absForWindows) {
            return "../../../../testResources/$resourceName"
        } else if ((Platform.isJsOrWasm() && BuildConfig.isKorlibs) || (Platform.isWasmJs())) {
            return "https://raw.githubusercontent.com/fleeksoft/ksoup/release/ksoup-test/testResources/$resourceName"
        }
        return "${BuildConfig.PROJECT_ROOT}/ksoup-test/testResources/$resourceName"
    }

    suspend fun readResourceAsString(resourceName: String): String {
        val bytes: ByteArray = if (resourceName.endsWith(".gz")) {
            readGzipFile(resourceName).readAllBytes()
        } else {
            readFile(resourceName).readAllBytes()
        }
        return bytes.decodeToString()
    }

    suspend fun resourceFilePathToStream(resource: String): SourceReader {
        return if (resource.endsWith(".gz") || resource.endsWith(".z")) {
            readGzipFile(resource)
        } else {
            readFile(resource)
        }
    }

    private suspend fun readFile(resource: String): SourceReader {
        val abs = getResourceAbsolutePath(resource, absForWindows = false)
        val bytes = if (abs.startsWith("https://", ignoreCase = true)) {
            abs.uniVfs.readAll()
        } else {
            SystemFileSystem.source(Path(abs)).buffered().readByteArray()
        }
        return bytes.openSourceReader()
    }

    private suspend fun readGzipFile(resource: String): SourceReader {
        val abs = getResourceAbsolutePath(resource, absForWindows = false)
        val bytes = if (abs.startsWith("https://", ignoreCase = true)) {
            abs.uniVfs.readAll()
        } else {
            SystemFileSystem.source(Path(abs)).buffered().readByteArray()
        }
        return bytes.uncompress(GZIP).openSourceReader()
    }

    fun dataToStream(
        data: String,
        charset: String,
    ): SourceReader {
        if (BuildConfig.isLite) {
            return data.encodeToByteArray().openSourceReader()
        }
        return data.toByteArray(Charsets.forName(charset)).openSourceReader()
    }

    fun isGzipSupported(): Boolean = BuildConfig.isKorlibs
    fun isUtf16Supported(): Boolean = !(((BuildConfig.isKotlinx || BuildConfig.isOkio || BuildConfig.isKtor2) && Platform.isJsOrWasm()) || BuildConfig.isLite)
    fun isUtf32Supported(): Boolean = !(Platform.isJsOrWasm() || Platform.isWindows() || Platform.isLinux())
    fun isEUCKRSupported(): Boolean = !(Platform.isJsOrWasm() || Platform.isApple() || Platform.isWindows() || (BuildConfig.isKorlibs && Platform.isLinux()))
    fun isGB2312Supported(): Boolean = !(BuildConfig.isLite || Platform.isApple() || Platform.isWindows() || ((BuildConfig.isKotlinx || BuildConfig.isOkio || BuildConfig.isKtor2) && Platform.isJsOrWasm()) || (BuildConfig.isKorlibs && Platform.isLinux()))

    fun canReadResourceFile(): Boolean = (!Platform.isWasmJs() || BuildConfig.isKorlibs) && !BuildConfig.isLite

    fun isFileSourceSupported(): Boolean = !BuildConfig.isLite
}