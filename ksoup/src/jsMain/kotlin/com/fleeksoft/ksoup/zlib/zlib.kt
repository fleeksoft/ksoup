package com.fleeksoft.ksoup.zlib

internal fun initializeZlib() {
    if (js("typeof global.zlib === 'undefined'") as Boolean) {
        js("global.zlib = require('zlib')")
    }
}

internal external object zlib {
    fun gunzip(
        buffer: ByteArray,
        callback: (err: Error?, result: ByteArray) -> Unit,
    )

    fun gunzipSync(buffer: ByteArray): ByteArray
}
