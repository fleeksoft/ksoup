package com.fleeksoft.ksoup

import okio.BufferedSource
import okio.Path

internal expect fun readGzipFile(file: Path): BufferedSource

internal expect fun readFile(file: Path): BufferedSource

// js don't support ?i
internal fun jsSupportedRegex(regex: String): Regex {
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
}

public expect object Platform {
    public val current: PlatformType
}

public fun Platform.isApple(): Boolean = this.current == PlatformType.IOS || this.current == PlatformType.MAC

public fun Platform.isWindows(): Boolean = this.current == PlatformType.WINDOWS

public fun Platform.isJvmOrAndroid(): Boolean = this.current == PlatformType.JVM || this.current == PlatformType.ANDROID

public fun Platform.isJvm(): Boolean = this.current == PlatformType.JVM

public fun Platform.isJS(): Boolean = this.current == PlatformType.JS
