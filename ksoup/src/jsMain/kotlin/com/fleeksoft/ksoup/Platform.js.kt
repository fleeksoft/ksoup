package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.zlib.initializeZlib
import com.fleeksoft.ksoup.zlib.zlib
import okio.*

internal actual fun readGzipFile(file: Path): BufferedSource {
    initializeZlib()
    return Buffer().apply {
        write(zlib.gunzipSync(readFile(file).readByteArray()))
    }
}

internal actual fun readFile(file: Path): BufferedSource {
    return NodeJsFileSystem.source(file).buffer()
}

public actual object Platform {
    public actual val current: PlatformType
        get() = PlatformType.JS
}
