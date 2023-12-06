package com.fleeksoft.ksoup

import okio.BufferedSource
import okio.NodeJsFileSystem
import okio.Path
import okio.buffer

internal actual fun readGzipFile(file: Path): BufferedSource {
    TODO("not implemented")
}

internal actual fun readFile(file: Path): BufferedSource {
    return NodeJsFileSystem.source(file).buffer()
}

public actual object Platform {
    public actual val current: PlatformType
        get() = PlatformType.JS
}
