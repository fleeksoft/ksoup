package com.fleeksoft.ksoup.io

interface Charset {
    val name: String
    // TODO: handle it better
    // some charsets can read but not encode; switch to an encodable charset and update the meta el
    fun canEncode(): Boolean = true

    fun canEncode(c: Char): Boolean {
        return this.canEncode("$c")
    }

    fun canEncode(s: String): Boolean {
    return true
        // TODO: check this
        /*return runCatching { this.encode(ByteArrayBuilder(s.length * 8), s) }
            .onFailure {
                println("encodingError: $this")
                it.printStackTrace()
            }.isSuccess*/
    }
    
    fun decode(stringBuilder: StringBuilder, byteArray: ByteArray, start: Int, end: Int = byteArray.size): Int
    fun toByteArray(value: String): ByteArray

    fun onlyUtf8(): Boolean = false
}