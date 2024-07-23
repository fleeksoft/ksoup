package com.fleeksoft.ksoup

import kotlinx.io.RawSource
import kotlinx.io.files.Path

internal actual fun readGzipFile(file: Path): RawSource {
    TODO("Not yet implemented")
}

public actual object Platform {
    public actual val current: PlatformType
        get() = PlatformType.WINDOWS
}
