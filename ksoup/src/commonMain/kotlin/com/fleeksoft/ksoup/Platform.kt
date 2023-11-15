package com.fleeksoft.ksoup

import io.ktor.client.engine.*
import okio.BufferedSource
import okio.Path

expect fun readGzipFile(file: Path): BufferedSource


expect fun readFile(file: Path): BufferedSource


expect fun provideHttpClientEngine(): HttpClientEngine