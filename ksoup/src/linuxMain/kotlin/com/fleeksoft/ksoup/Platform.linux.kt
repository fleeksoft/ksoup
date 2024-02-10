package com.fleeksoft.ksoup

/*internal actual fun readGzipFile(file: Path): BufferedSource {
    TODO("gzip Not yet supported")
}

internal actual fun readFile(file: Path): BufferedSource {
    FileSystem.SYSTEM.source(file).buffer()
    return FileSystem.SYSTEM.source(file).buffer()
}*/

public actual object Platform {
    public actual val current: PlatformType
        get() = PlatformType.LINUX
}
