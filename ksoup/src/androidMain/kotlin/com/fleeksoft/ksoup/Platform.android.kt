package com.fleeksoft.ksoup

import io.ktor.utils.io.streams.*
import kotlinx.io.RawSource
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import java.util.zip.GZIPInputStream

internal actual fun readGzipFile(file: Path): RawSource {
    return GZIPInputStream(SystemFileSystem.source(file).buffered().inputStream()).asSource()
}

public actual object Platform {
    public actual val current: PlatformType
        get() = PlatformType.ANDROID
}
