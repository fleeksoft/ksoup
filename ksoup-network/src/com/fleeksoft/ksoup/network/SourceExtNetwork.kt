@file:OptIn(InternalAPI::class)

package com.fleeksoft.ksoup.network

import com.fleeksoft.ksoup.io.SourceReader
import com.fleeksoft.ksoup.io.from
import io.ktor.client.statement.*
import io.ktor.utils.io.*

suspend fun HttpResponse.asSourceReader() = SourceReader.from(this.bodyAsChannel().readBuffer)