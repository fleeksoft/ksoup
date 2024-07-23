package com.fleeksoft.ksoup

import kotlinx.io.RawSource
import kotlinx.io.files.Path

internal actual fun readGzipFile(file: Path): RawSource {
    TODO("gzip Not yet supported")
}

public actual object Platform {
    public actual val current: PlatformType
        get() = PlatformType.LINUX
}
