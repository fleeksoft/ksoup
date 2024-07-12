package com.fleeksoft.ksoup

import korlibs.io.compression.deflate.GZIP
import korlibs.io.compression.uncompress
import korlibs.io.file.readAsSyncStream
import korlibs.io.file.std.uniVfs
import korlibs.io.stream.*

public suspend fun readGzipFile(filePath: String): SyncStream {
    return filePath.uniVfs.readAsSyncStream().readAll().uncompress(GZIP).openSync()
}

public suspend fun readFile(filePath: String): SyncStream {
    return filePath.uniVfs.readAsSyncStream()
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
    WASMJS,
}

public expect object Platform {
    public val current: PlatformType
}

public fun Platform.isApple(): Boolean = this.current == PlatformType.IOS || this.current == PlatformType.MAC

public fun Platform.isWindows(): Boolean = this.current == PlatformType.WINDOWS

public fun Platform.isJvmOrAndroid(): Boolean = this.current == PlatformType.JVM || this.current == PlatformType.ANDROID

public fun Platform.isJvm(): Boolean = this.current == PlatformType.JVM

public fun Platform.isJS(): Boolean = this.current == PlatformType.JS
