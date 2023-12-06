package com.fleeksoft.ksoup

import okio.*

internal actual fun readGzipFile(file: Path): BufferedSource {
    return readFile(file).gzip().buffer()
}

internal actual fun readFile(file: Path): BufferedSource {
    return FileSystem.SYSTEM.source(file).buffer()
}

public actual object Platform {
    public actual val current: PlatformType
        get() = PlatformType.ANDROID
}
