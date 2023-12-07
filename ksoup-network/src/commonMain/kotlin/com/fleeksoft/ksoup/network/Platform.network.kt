package com.fleeksoft.ksoup.network

import io.ktor.client.engine.HttpClientEngine

internal expect fun provideHttpClientEngine(): HttpClientEngine
