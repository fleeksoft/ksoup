package com.fleeksoft.ksoup.io

import okio.FileSystem
import okio.Path
import okio.SYSTEM
import okio.Source

actual fun readFile(file: Path): Source {
    return FileSystem.SYSTEM.source(file)
}