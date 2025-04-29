/*
 * Kotlin port of jsoup's Token.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.Normalizer
import com.fleeksoft.ksoup.nodes.Attributes
import com.fleeksoft.ksoup.nodes.Range
import com.fleeksoft.ksoup.ported.assert
import kotlin.js.JsName

/**
 * Parse tokens for the Tokeniser.
 */
public abstract class Token private constructor(public var type: TokenType) {
    @JsName("_startPos")
    protected var startPos = 0
    @JsName("_endPos")
    protected var endPos = UnsetPos // position in CharacterReader this token was read from

    public fun tokenType(): String {
        return this::class.simpleName ?: "Token"
    }

    /**
     * Reset the data represent by this token, for reuse. Prevents the need to create transfer objects for every
     * piece of data, which immediately get GCed.
     */
    open fun reset(): Token {
        startPos = UnsetPos
        endPos = UnsetPos
        return this
    }

    public fun startPos(): Int {
        return startPos
    }

    public fun startPos(pos: Int) {
        startPos = pos
    }

    public fun endPos(): Int {
        return endPos
    }

    public fun endPos(pos: Int) {
        endPos = pos
    }

    public class Doctype : Token(TokenType.Doctype) {
        public val name: TokenData = TokenData()
        public var pubSysKey: String? = null
        public val publicIdentifier: TokenData = TokenData()
        public val systemIdentifier: TokenData = TokenData()
        public var forceQuirks: Boolean = false

        override fun reset(): Token {
            super.reset()
            name.reset()
            pubSysKey = null
            publicIdentifier.reset()
            systemIdentifier.reset()
            forceQuirks = false
            return this
        }

        public fun getName(): String {
            return name.value()
        }

        public fun getPublicIdentifier(): String {
            return publicIdentifier.value()
        }

        public fun getSystemIdentifier(): String {
            return systemIdentifier.value()
        }

        override fun toString(): String {
            return "<!doctype ${getName()}>"
        }
    }

    public abstract class Tag(type: TokenType, public val treeBuilder: TreeBuilder) : Token(type) {
        internal var tagName: TokenData = TokenData()
        internal var normalName: String? = null // lc version of tag name, for case-insensitive tree build
        public var selfClosing: Boolean = false

        // start tags get attributes on construction. End tags get attributes on first new attribute (but only for parser convenience, not used).
        public var attributes: Attributes? = null

        private val attrName = TokenData()
        private val attrValue = TokenData()
        private var hasEmptyAttrValue = false // distinguish boolean attribute from empty string value

        internal val trackSource: Boolean = treeBuilder.trackSourceRange
        private var attrNameStart: Int = 0
        private var attrNameEnd: Int = 0
        private var attrValStart: Int = 0
        private var attrValEnd: Int = 0

        override fun reset(): Tag {
            super.reset()
            tagName.reset()
            normalName = null
            selfClosing = false
            attributes = null
            resetPendingAttr()
            return this
        }

        private fun resetPendingAttr() {
            attrName.reset()
            attrValue.reset()
            hasEmptyAttrValue = false

            if (trackSource) {
                attrValEnd = UnsetPos
                attrValStart = attrValEnd
                attrNameEnd = attrValStart
                attrNameStart = attrNameEnd
            }
        }

        public fun newAttribute() {
            if (attributes == null) attributes = Attributes()

            if (attrName.hasData() && attributes!!.size() < MaxAttributes) {
                // the tokeniser has skipped whitespace control chars, but trimming could collapse to empty for other control codes, so verify here
                var name = attrName.value()
                name = name.trim { it <= ' ' }
                if (!name.isEmpty()) {
                    val value: String?
                    if (attrValue.hasData()) value = attrValue.value()
                    else if (hasEmptyAttrValue) value = ""
                    else value = null
                    // note that we add, not put. So that the first is kept, and rest are deduped, once in a context where case sensitivity is known, and we can warn for duplicates.
                    attributes!!.add(name, value)

                    trackAttributeRange(name)
                }
            }
            resetPendingAttr()
        }

        private fun trackAttributeRange(name: String) {
            if (trackSource && isStartTag()) {
                val start = asStartTag()
                val r = start.treeBuilder.reader
                val preserve = start.treeBuilder.settings!!.preserveAttributeCase()

                assert(attributes != null)

                var name = name
                if (!preserve) name = Normalizer.lowerCase(name)
                if (attributes!!.sourceRange(name).nameRange().isTracked()) {
                    return // dedupe ranges as we go; actual attributes get deduped later for error count
                }

                // if there's no value (e.g. boolean), make it an implicit range at current
                if (!attrValue.hasData()) {
                    attrValEnd = attrNameEnd
                    attrValStart = attrValEnd
                }

                val range: Range.AttributeRange = Range.AttributeRange(
                    Range(
                        Range.Position(attrNameStart, r.lineNumber(attrNameStart), r.columnNumber(attrNameStart)),
                        Range.Position(attrNameEnd, r.lineNumber(attrNameEnd), r.columnNumber(attrNameEnd)),
                    ),
                    Range(
                        Range.Position(attrValStart, r.lineNumber(attrValStart), r.columnNumber(attrValStart)),
                        Range.Position(attrValEnd, r.lineNumber(attrValEnd), r.columnNumber(attrValEnd)),
                    ),
                )
                attributes!!.sourceRange(name, range)
            }
        }

        public fun hasAttributes(): Boolean {
            return attributes != null
        }

        public fun hasAttributeIgnoreCase(key: String?): Boolean {
            return attributes != null && attributes!!.hasKeyIgnoreCase(key!!)
        }

        public fun finaliseTag() {
            // finalises for emit
            if (attrName.hasData()) {
                newAttribute()
            }
        }

        /** Preserves case  */
        public fun name(): String { // preserves case, for input into Tag.valueOf (which may drop case)
            return tagName.value()
        }

        /** Lower case  */
        public fun retrieveNormalName(): String { // lower case, used in tree building for working out where in tree it should go
            Validate.isFalse(normalName == null || normalName!!.isEmpty());
            return normalName ?: ""
        }

        public fun toStringName(): String {
            val name = tagName.value()
            return name.ifEmpty { "[unset]" }
        }

        public fun name(name: String): Tag {
            tagName.set(name)
            normalName = ParseSettings.normalName(tagName.value())
            return this
        }

        // these appenders are rarely hit in not null state-- caused by null chars.
        public fun appendTagName(append: String) {
            // might have null chars - need to replace with null replacement character

            // might have null chars - need to replace with null replacement character
            val append = append.replace(TokeniserState.nullChar, Tokeniser.ReplacementChar)
            tagName.append(append)
            normalName = ParseSettings.normalName(tagName.value())
        }

        public fun appendTagName(append: Char) {
            appendTagName(append.toString()) // so that normalname gets updated too
        }

        public fun appendAttributeName(append: String, startPos: Int, endPos: Int) {
            // might have null chars because we eat in one pass - need to replace with null replacement character
            val append = append.replace(TokeniserState.nullChar, Tokeniser.ReplacementChar)
            attrName.append(append)
            attrNamePos(startPos, endPos)
        }

        public fun appendAttributeName(append: Char, startPos: Int, endPos: Int) {
            attrName.append(append)
            attrNamePos(startPos, endPos)
        }

        public fun appendAttributeValue(append: String, startPos: Int, endPos: Int) {
            attrValue.append(append)
            attrValPos(startPos, endPos)
        }

        public fun appendAttributeValue(append: Char, startPos: Int, endPos: Int) {
            attrValue.append(append)
            attrValPos(startPos, endPos)
        }

        public fun appendAttributeValue(appendCodepoints: IntArray, startPos: Int, endPos: Int) {
            for (codepoint in appendCodepoints) {
                attrValue.appendCodePoint(codepoint)
            }
            attrValPos(startPos, endPos)
        }

        public fun setEmptyAttributeValue() {
            hasEmptyAttrValue = true
        }

        private fun attrNamePos(startPos: Int, endPos: Int) {
            if (trackSource) {
                attrNameStart = if (attrNameStart > UnsetPos) attrNameStart else startPos // latches to first
                attrNameEnd = endPos
            }
        }

        private fun attrValPos(startPos: Int, endPos: Int) {
            if (trackSource) {
                attrValStart = if (attrValStart > UnsetPos) attrValStart else startPos // latches to first
                attrValEnd = endPos
            }
        }

        abstract override fun toString(): String

        public companion object {
            /* Limits runaway crafted HTML from spewing attributes and getting a little sluggish in ensureCapacity.
        Real-world HTML will P99 around 8 attributes, so plenty of headroom. Implemented here and not in the Attributes
        object so that API users can add more if ever required. */
            private const val MaxAttributes = 512
        }
    }

    // TreeBuilder is provided so if tracking, can get line / column positions for Range; and can dedupe as we go
    public class StartTag(treeBuilder: TreeBuilder) : Tag(TokenType.StartTag, treeBuilder) {
        override fun reset(): Tag {
            super.reset()
            attributes = null
            return this
        }

        public fun nameAttr(name: String, attributes: Attributes?): StartTag {
            this.tagName.set(name)
            this.attributes = attributes
            normalName = ParseSettings.normalName(name)
            return this
        }

        override fun toString(): String {
            val closer = if (selfClosing) "/>" else ">"
            return if (hasAttributes() && attributes!!.size() > 0) {
                "<${toStringName()} $attributes$closer"
            } else {
                "<${toStringName()}$closer"
            }
        }
    }

    public class EndTag(treeBuilder: TreeBuilder) : Tag(TokenType.EndTag, treeBuilder) {
        override fun toString(): String {
            return "</${toStringName()}>"
        }
    }

    public class Comment : Token(TokenType.Comment) {
        private val data = TokenData()
        public var bogus: Boolean = false

        override fun reset(): Token {
            super.reset()
            data.reset()
            bogus = false
            return this
        }

        public fun getData(): String {
            return data.value()
        }

        public fun append(append: String): Comment {
            data.append(append)
            return this
        }

        public fun append(append: Char): Comment {
            data.append(append)
            return this
        }

        override fun toString(): String {
            return "<!--${getData()}-->"
        }
    }

    public open class Character() : Token(TokenType.Character) {
        val data: TokenData = TokenData()

        /** Deep copy */
        constructor(source: Character) : this() {
            this.startPos = source.startPos
            this.endPos = source.endPos
            this.data.set(source.data.value())
        }

        override fun reset(): Token {
            super.reset()
            data.reset()
            return this
        }

        public fun data(str: String): Character {
            this.data.set(str)
            return this
        }

        fun append(str: String): Character {
            data.append(str)
            return this
        }

        fun getData(): String = data.value()

        override fun toString(): String = getData()
    }

    internal class CData(data: String) : Character() {
        init {
            this.data(data)
        }

        override fun toString(): String {
            return "<![CDATA[$data]]>"
        }
    }

    /**
     * XmlDeclaration - extends Tag for pseudo attribute support
     */
    class XmlDecl(treeBuilder: TreeBuilder) : Tag(TokenType.XmlDecl, treeBuilder) {
        var isDeclaration: Boolean = true // <!..>, or <?...?> if false (a processing instruction)

        override fun reset(): XmlDecl {
            super.reset()
            isDeclaration = true
            return this
        }

        override fun toString(): String {
            val open = if (isDeclaration) "<!" else "<?"
            val close = if (isDeclaration) ">" else "?>"
            return if (hasAttributes() && attributes!!.size > 0)
                "$open${toStringName()} ${attributes}$close"
            else
                "$open${toStringName()}$close"
        }
    }

    internal class EOF : Token(TokenType.EOF) {
        override fun reset(): Token {
            super.reset()
            return this
        }

        override fun toString(): String {
            return ""
        }
    }

    public fun isDoctype(): Boolean {
        return type == TokenType.Doctype
    }

    public fun asDoctype(): Doctype {
        return this as Doctype
    }

    public fun isStartTag(): Boolean {
        return type == TokenType.StartTag
    }

    public fun asStartTag(): StartTag {
        return this as StartTag
    }

    public fun isEndTag(): Boolean {
        return type == TokenType.EndTag
    }

    public fun asEndTag(): EndTag {
        return this as EndTag
    }

    public fun isComment(): Boolean {
        return type == TokenType.Comment
    }

    public fun asComment(): Comment {
        return this as Comment
    }

    public fun isCharacter(): Boolean {
        return type == TokenType.Character
    }

    public fun isCData(): Boolean {
        return this is CData
    }

    public fun asCharacter(): Character {
        return this as Character
    }

    public fun asXmlDecl(): XmlDecl {
        return this as XmlDecl
    }

    public fun isEOF(): Boolean {
        return type == TokenType.EOF
    }

    public enum class TokenType {
        Doctype,
        StartTag,
        EndTag,
        Comment,
        Character, // note no CData - treated in builder as an extension of Character
        XmlDecl,
        EOF,
    }

    public companion object {
        public const val UnsetPos: Int = -1

        public fun reset(sb: StringBuilder?) {
            sb?.clear()
        }
    }
}
