package com.fleeksoft.ksoup.io

import okio.Path
import okio.Source

actual fun readFile(file: Path): Source {
    throw UnsupportedOperationException("readFile is not supported on WebAssembly (wasm) platforms in Okio.")
}