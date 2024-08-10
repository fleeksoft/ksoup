package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.ported.io.BufferReader
import com.fleeksoft.ksoup.ported.io.openBufferReader
import korlibs.io.compression.deflate.GZIP
import korlibs.io.compression.uncompress
import korlibs.io.file.VfsFile
import korlibs.io.file.readAsSyncStream
import korlibs.io.stream.readAll

public suspend fun readGzipFile(file: VfsFile): BufferReader {
    return file.readAsSyncStream().readAll().uncompress(GZIP).openBufferReader()
}

public suspend fun readFile(file: VfsFile): BufferReader {
    return file.openBufferReader()
}

// js don't support ?i
public fun jsSupportedRegex(regex: String): Regex {
    return if (Platform.isJS() && regex.contains("(?i)")) {
        Regex(regex.replace("(?i)", ""), RegexOption.IGNORE_CASE)
    } else {
        Regex(regex)
    }
}

public enum class PlatformType {
    ANDROID,
    JVM,
    IOS,
    LINUX,
    JS,
    MAC,
    WINDOWS,
    WASM_JS,
}

public expect object Platform {
    public val current: PlatformType
}

public fun Platform.isApple(): Boolean = this.current == PlatformType.IOS || this.current == PlatformType.MAC

public fun Platform.isWindows(): Boolean = this.current == PlatformType.WINDOWS

public fun Platform.isJvmOrAndroid(): Boolean = this.current == PlatformType.JVM || this.current == PlatformType.ANDROID

public fun Platform.isJvm(): Boolean = this.current == PlatformType.JVM

public fun Platform.isJS(): Boolean = this.current == PlatformType.JS || this.current == PlatformType.WASM_JS

public fun Platform.isWasmJs(): Boolean = this.current == PlatformType.WASM_JS
