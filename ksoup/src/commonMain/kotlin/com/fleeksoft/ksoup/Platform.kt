package com.fleeksoft.ksoup

import io.ktor.client.engine.*
import okio.BufferedSource
import okio.Path

internal expect fun readGzipFile(file: Path): BufferedSource


internal expect fun readFile(file: Path): BufferedSource


internal expect fun provideHttpClientEngine(): HttpClientEngine