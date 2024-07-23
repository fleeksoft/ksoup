package com.fleeksoft.ksoup

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.readByteArray

internal actual fun readGzipFile(file: Path): RawSource {
    // TODO: optimize for BufferedSource without reading all bytes
    return Buffer().apply {
        write(decompressGzip(readFile(file).buffered().readByteArray()))
    }
}

public actual object Platform {
    public actual val current: PlatformType
        get() = PlatformType.IOS
}
