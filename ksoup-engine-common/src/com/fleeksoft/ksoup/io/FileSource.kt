package com.fleeksoft.ksoup.io

import com.fleeksoft.io.InputStream

interface FileSource {
    suspend fun asInputStream(): InputStream
    fun getPath(): String
    fun getFullName(): String

    companion object
}