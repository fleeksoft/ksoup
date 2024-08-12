package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.ported.io.Charset

//todo: set defualt charset to UTF8
fun String.toByteArray(charset: Charset): ByteArray = charset.toByteArray(this)

fun ByteArray.toString(charset: Charset, start: Int = 0, end: Int = this.size): String =
    charset.toString(this, start, end)