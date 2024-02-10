package com.fleeksoft.ksoup

/*internal actual fun readGzipFile(file: VfsFile): BufferedSource {
    return readFile(file).gzip().buffer()
}

internal actual fun readFile(file: VfsFile): BufferedSource {
    return FileSystem.SYSTEM.source(file).buffer()
}*/

public actual object Platform {
    public actual val current: PlatformType
        get() = PlatformType.JVM
}
