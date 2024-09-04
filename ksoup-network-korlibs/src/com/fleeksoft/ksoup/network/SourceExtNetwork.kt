package com.fleeksoft.ksoup.network

import com.fleeksoft.ksoup.io.SourceReader
import com.fleeksoft.ksoup.io.from
import korlibs.io.net.http.HttpClient
import korlibs.io.stream.readAll

suspend fun HttpClient.Response.asSourceReader() = SourceReader.from(this.content.readAll())