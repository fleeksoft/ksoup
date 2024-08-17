package com.fleeksoft.ksoup.kotlinx.ported.io

abstract class CharBuffer: Appendable {
    abstract fun hasArray(): Boolean
    abstract fun array(): CharArray
    abstract fun position(): Int
    abstract fun limit(): Int
    abstract fun arrayOffset(): Int
    abstract fun remaining(): Int
    abstract fun put(cbuf: CharArray, i: Int, nread: Int)
    abstract fun setPosition(post: Int)
    abstract fun slice(): CharBuffer
    abstract fun hasRemaining(): Boolean


    companion object {
        fun wrap(
            array: CharArray,
            offset: Int, length: Int
        ): CharBuffer {
            TODO()
        }
    }
}