package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.internal.SharedConstants

/**
 * A Range object tracks the character positions in the original input source where a Node starts or ends. If you want to
 * track these positions, tracking must be enabled in the Parser with
 * [com.fleeksoft.ksoup.parser.Parser.setTrackPosition].
 * @see Node.sourceRange
 *
 * Creates a new Range with start and end Positions. Called by TreeBuilder when position tracking is on.
 * @param start the start position
 * @param end the end position
 */
public data class Range(private val start: Position, private val end: Position) {
    /**
     * Get the start position of this node.
     * @return the start position
     */
    public fun start(): Position {
        return start
    }

    /**
     * Get the end position of this node.
     * @return the end position
     */
    public fun end(): Position {
        return end
    }

    /**
     * Get the starting cursor position of this range.
     * @return the 0-based start cursor position.
     */
    public fun startPos(): Int {
        return start.pos
    }

    /**
     * Get the ending cursor position of this range.
     * @return the 0-based ending cursor position.
     */
    public fun endPos(): Int {
        return end.pos
    }

    public fun isTracked(): Boolean = this != Untracked

    /**
     * Checks if the range represents a node that was implicitly created / closed.
     *
     * For example, with HTML of `<p>One<p>Two`, both `p` elements will have an explicit
     * [Element.sourceRange] but an implicit [Element.endSourceRange] marking the end position, as neither
     * have closing `</p>` tags. The TextNodes will have explicit sourceRanges.
     *
     * A range is considered implicit if its start and end positions are the same.
     * @return true if the range is tracked and its start and end positions are the same, false otherwise.
     */
    public fun isImplicit(): Boolean {
        if (!isTracked()) return false
        return start == end
    }

    /**
     * Gets a String presentation of this Range, in the format `line,column:pos-line,column:pos`.
     * @return a String
     */
    override fun toString(): String {
        return "$start-$end"
    }

    /**
     * A Position object tracks the character position in the original input source where a Node starts or ends. If you want to
     * track these positions, tracking must be enabled in the Parser with
     * [com.fleeksoft.ksoup.parser.Parser.setTrackPosition].
     * @see Node.sourceRange
     *
     * Create a new Position object. Called by the TreeBuilder if source position tracking is on.
     * @param pos position index
     * @param lineNumber line number
     * @param columnNumber column number
     */
    public data class Position(internal val pos: Int, private val lineNumber: Int, private val columnNumber: Int) {
        /**
         * Gets the position index (0-based) of the original input source that this Position was read at. This tracks the
         * total number of characters read into the source at this position, regardless of the number of preceding lines.
         * @return the position, or `-1` if untracked.
         */
        public fun pos(): Int {
            return pos
        }

        /**
         * Gets the line number (1-based) of the original input source that this Position was read at.
         * @return the line number, or `-1` if untracked.
         */
        public fun lineNumber(): Int {
            return lineNumber
        }

        /**
         * Gets the cursor number (1-based) of the original input source that this Position was read at. The cursor number
         * resets to 1 on every new line.
         * @return the cursor number, or `-1` if untracked.
         */
        public fun columnNumber(): Int {
            return columnNumber
        }

        public fun isTracked(): Boolean = this != UntrackedPos

        /**
         * Gets a String presentation of this Position, in the format `line,column:pos`.
         * @return a String
         */
        override fun toString(): String {
            return "$lineNumber,$columnNumber:$pos"
        }
    }

    public data class AttributeRange(private val nameRange: Range, private val valueRange: Range) {
        /** Get the source range for the attribute's name.  */
        public fun nameRange(): Range {
            return nameRange
        }

        /** Get the source range for the attribute's value.  */
        public fun valueRange(): Range {
            return valueRange
        }

        /** Get a String presentation of this Attribute range, in the form
         * `line,column:pos-line,column:pos=line,column:pos-line,column:pos` (name start - name end = val start - val end).
         * .  */
        override fun toString(): String {
            return "${nameRange()}=${valueRange()}"
        }

        public companion object {
            public val UntrackedAttr: AttributeRange = AttributeRange(Untracked, Untracked)
        }
    }

    public companion object {
        private val UntrackedPos = Position(-1, -1, -1)

        /** An untracked source range. */
        private val Untracked = Range(UntrackedPos, UntrackedPos)

        /**
         * Retrieves the source range for a given Node.
         * @param node the node to retrieve the position for
         * @param start if this is the starting range. `false` for Element end tags.
         * @return the Range, or the Untracked (-1) position if tracking is disabled.
         */
        public fun of(
            node: Node,
            start: Boolean,
        ): Range {
            val key: String = if (start) SharedConstants.RangeKey else SharedConstants.EndRangeKey
            if (!node.hasAttributes()) return Untracked
            val range = node.attributes().userData(key)
            return if (range != null) range as Range else Untracked
        }
    }
}
