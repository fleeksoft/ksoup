package com.fleeksoft.ksoup.parser

/**
 * A Parse Error records an error in the input HTML that occurs in either the tokenisation or the tree building phase.
 */
internal class ParseError {
    val pos: Int
    val cursorPos: String
    val errorMsg: String

    internal constructor(reader: CharacterReader, errorMsg: String) {
        pos = reader.pos()
        cursorPos = reader.posLineCol()
        this.errorMsg = errorMsg
    }

    internal constructor(pos: Int, errorMsg: String) {
        this.pos = pos
        cursorPos = pos.toString()
        this.errorMsg = errorMsg
    }

    override fun toString(): String {
        return "<$cursorPos>: $errorMsg"
    }
}
