package com.fleeksoft.ksoup.ported.io

interface Buffer {

    public val size: Int
    public fun position(): Int
    public fun available(): Int
    public fun compact()

    fun exhausted(): Boolean
    fun readText(charset: Charset = Charsets.UTF8, max: Int = Int.MAX_VALUE): String

    fun writeBytes(byteArray: ByteArray, length: Int)
}