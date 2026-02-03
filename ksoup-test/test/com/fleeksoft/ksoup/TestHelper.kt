package com.fleeksoft.ksoup

import com.fleeksoft.io.InputStream
import com.fleeksoft.io.inputStream
import com.fleeksoft.io.kotlinx.asInputStream
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import korlibs.io.compression.deflate.GZIP
import korlibs.io.compression.uncompress
import korlibs.io.file.std.uniVfs
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

object TestHelper {

    suspend fun readGzipResource(resource: String): InputStream {
        return readGzipFile(resource)
    }

    suspend fun readResource(resource: String): InputStream {
        if (resource.endsWith(".gz") || resource.endsWith(".z")) {
            return readGzipResource(resource)
        }
        return readFile(resource)
    }

    fun getResourceAbsolutePath(resourceName: String, absForWindows: Boolean = true): String {
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

    suspend fun resourceFilePathToStream(resource: String): InputStream {
        return if (resource.endsWith(".gz") || resource.endsWith(".z")) {
            readGzipFile(resource)
        } else {
            readFile(resource)
        }
    }

    private suspend fun readFile(resource: String): InputStream {
        val abs = getResourceAbsolutePath(resource, absForWindows = false)
        return if (abs.startsWith("https://", ignoreCase = true)) {
            abs.uniVfs.readAll().inputStream()
        } else {
            SystemFileSystem.source(Path(abs)).buffered().asInputStream()
        }
    }

    private suspend fun readGzipFile(resource: String): InputStream {
        val abs = getResourceAbsolutePath(resource, absForWindows = false)
        val bytes = if (abs.startsWith("https://", ignoreCase = true)) {
            abs.uniVfs.readAll()
        } else {
            SystemFileSystem.source(Path(abs)).buffered().readByteArray()
        }
        return bytes.uncompress(GZIP).inputStream()
    }

    suspend fun parseResource(
        resourceName: String,
        baseUri: String = "",
        charsetName: String? = null,
        parser: Parser = Parser.htmlParser()
    ): Document {
        return if (!canReadResourceFile() || (!isGzipSupported() &&
                    (resourceName.endsWith(".gz") || resourceName.endsWith(".z")))
        ) {
            val input = readResource(resourceName)
            Ksoup.parseInput(input = input, baseUri = baseUri, charsetName = charsetName, parser = parser)
        } else {
            val input: String = getResourceAbsolutePath(resourceName)
            Ksoup.parseFile(filePath = input, charsetName = charsetName, baseUri = baseUri, parser = parser)
        }
    }

    fun isGzipSupported(): Boolean = false
    fun isShiftJsSupported(): Boolean = Platform.isJvmOrAndroid()

    //    fun isUtf16Supported(): Boolean = !(((BuildConfig.isKotlinx || BuildConfig.isOkio || BuildConfig.isKtor2) && Platform.isJsOrWasm()))
    fun isUtf16Supported(): Boolean = true

    //    fun isUtf32Supported(): Boolean = !(Platform.isJsOrWasm() || Platform.isWindows() || Platform.isLinux())
    fun isUtf32Supported(): Boolean = true

    fun isEUCKRSupported(): Boolean = true

    fun isGB2312Supported(): Boolean = true

    fun canReadResourceFile(): Boolean = Platform.isWasmJs().not() && BuildConfig.isCore.not()
}