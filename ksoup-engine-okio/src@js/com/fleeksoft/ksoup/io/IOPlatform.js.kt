package com.fleeksoft.ksoup.io

import okio.NodeJsFileSystem
import okio.Path
import okio.Source

actual fun readFile(file: Path): Source {
    return NodeJsFileSystem.source(file)
}