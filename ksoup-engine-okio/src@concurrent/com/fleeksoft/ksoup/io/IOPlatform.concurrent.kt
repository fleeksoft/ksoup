package com.fleeksoft.ksoup.io

import okio.FileSystem
import okio.Path
import okio.Source

actual fun readFile(path: Path): Source {
    return FileSystem.SYSTEM.source(path)
}