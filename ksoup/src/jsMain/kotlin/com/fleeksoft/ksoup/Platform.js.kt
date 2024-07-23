package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.zlib.initializeZlib
import com.fleeksoft.ksoup.zlib.zlib
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.readByteArray

internal actual fun readGzipFile(file: Path): RawSource {
    initializeZlib()
    return Buffer().apply {
        write(zlib.gunzipSync(readFile(file).buffered().readByteArray()))
    }
}

public actual object Platform {
    public actual val current: PlatformType
        get() = PlatformType.JS
}
