package com.fleeksoft.ksoup.network

import io.ktor.client.engine.*
import io.ktor.client.engine.winhttp.*

internal actual fun provideHttpClientEngine(): HttpClientEngine {
    return WinHttp.create()
}
