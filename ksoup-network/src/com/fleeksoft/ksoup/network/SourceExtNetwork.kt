@file:OptIn(InternalAPI::class)

package com.fleeksoft.ksoup.network

import com.fleeksoft.io.asInputStream
import com.fleeksoft.ksoup.io.SourceReader
import com.fleeksoft.ksoup.io.from
import io.ktor.client.statement.*
import io.ktor.utils.io.*

@Deprecated(
    message = "HttpResponse.asSourceReader() is deprecated, use HttpResponse.asInputStream() instead.",
    replaceWith = ReplaceWith("this.asInputStream()"),
    level = DeprecationLevel.WARNING
)
suspend fun HttpResponse.asSourceReader() = SourceReader.from(this.bodyAsChannel().readBuffer)
suspend fun HttpResponse.asInputStream() = this.bodyAsChannel().readBuffer.asInputStream()