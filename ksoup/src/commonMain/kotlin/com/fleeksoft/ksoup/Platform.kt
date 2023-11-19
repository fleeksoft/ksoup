package com.fleeksoft.ksoup

import okio.BufferedSource
import okio.Path

internal expect fun readGzipFile(file: Path): BufferedSource


internal expect fun readFile(file: Path): BufferedSource