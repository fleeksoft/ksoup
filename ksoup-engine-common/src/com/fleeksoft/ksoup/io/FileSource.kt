package com.fleeksoft.ksoup.io

interface FileSource {
    suspend fun toSourceReader(): SourceReader
    fun getPath(): String
    fun getFullName(): String
}