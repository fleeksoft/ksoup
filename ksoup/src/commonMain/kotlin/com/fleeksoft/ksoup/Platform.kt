package com.fleeksoft.ksoup

import okio.BufferedSource
import okio.Path

internal expect fun readGzipFile(file: Path): BufferedSource

internal expect fun readFile(file: Path): BufferedSource

// js don't support ?i
internal fun jsSupportedRegex(regex: String): Regex {
    return if (Platform.current == PlatformType.JS && regex.contains("(?i)")) {
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
