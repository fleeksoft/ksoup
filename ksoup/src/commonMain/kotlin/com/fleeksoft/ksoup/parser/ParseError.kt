package com.fleeksoft.ksoup.parser

/**
 * A Parse Error records an error in the input HTML that occurs in either the tokenisation or the tree building phase.
 */
internal class ParseError {
    /**
     * Retrieves the offset of the error.
     * @return error offset within input
     */
    val position: Int

    /**
     * Get the formatted line:column cursor position where the error occurred.
     * @return line:number cursor position
     */
    val cursorPos: String

    /**
     * Retrieve the error message.
     * @return the error message.
     */
    val errorMessage: String

    internal constructor(reader: CharacterReader, errorMsg: String) {
        position = reader.pos()
        cursorPos = reader.cursorPos()
        errorMessage = errorMsg
    }

    internal constructor(pos: Int, errorMsg: String) {
        position = pos
        cursorPos = pos.toString()
        errorMessage = errorMsg
    }

    override fun toString(): String {
        return "<$cursorPos>: $errorMessage"
    }
}
