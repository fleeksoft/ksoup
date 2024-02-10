package com.fleeksoft.ksoup

/*internal actual fun readGzipFile(file: Path): BufferedSource {
    // TODO: optimize for BufferedSource without reading all bytes
    return Buffer().apply {
        write(decompressGzip(readFile(file).buffer().readByteArray()))
    }
}

internal actual fun readFile(file: Path): BufferedSource {
    return FileSystem.SYSTEM.source(file).buffer()
}*/

public actual object Platform {
    public actual val current: PlatformType
        get() = PlatformType.IOS
}
