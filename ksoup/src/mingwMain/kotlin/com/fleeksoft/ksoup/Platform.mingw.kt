package com.fleeksoft.ksoup

import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.buffer

internal actual fun readGzipFile(file: Path): BufferedSource {
    TODO("Not yet implemented")
}

internal actual fun readFile(file: Path): BufferedSource {
    return FileSystem.SYSTEM.source(file).buffer()
}

public actual object Platform {
    public actual val current: PlatformType
        get() = PlatformType.WINDOWS
}
