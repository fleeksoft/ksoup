package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.Normalizer
import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.nodes.Attributes
import com.fleeksoft.ksoup.nodes.Range
import com.fleeksoft.ksoup.ported.KCloneable
import com.fleeksoft.ksoup.ported.appendCodePoint
import com.fleeksoft.ksoup.ported.assert

/**
 * Parse tokens for the Tokeniser.
 */
public abstract class Token private constructor(public var type: TokenType) {
    private var _startPos = 0
    private var _endPos = Unset // position in CharacterReader this token was read from

    public fun tokenType(): String {
        return this::class.simpleName ?: "Token"
    }

    /**
     * Reset the data represent by this token, for reuse. Prevents the need to create transfer objects for every
     * piece of data, which immediately get GCed.
     */
    public open fun reset(): Token {
        _startPos = Unset
        _endPos = Unset
        return this
    }

    public fun startPos(): Int {
        return _startPos
    }

    public fun startPos(pos: Int) {
        _startPos = pos
    }

    public fun endPos(): Int {
        return _endPos
    }

    public fun endPos(pos: Int) {
        _endPos = pos
    }

    public class Doctype : Token(TokenType.Doctype) {
        public val name: StringBuilder = StringBuilder()
        public var pubSysKey: String? = null
        public val publicIdentifier: StringBuilder = StringBuilder()
        public val systemIdentifier: StringBuilder = StringBuilder()
        public var isForceQuirks: Boolean = false

        override fun reset(): Token {
            super.reset()
            reset(name)
            pubSysKey = null
            reset(publicIdentifier)
            reset(systemIdentifier)
            isForceQuirks = false
            return this
        }

        public fun getName(): String {
            return name.toString()
        }

        public fun getPublicIdentifier(): String {
            return publicIdentifier.toString()
        }

        public fun getSystemIdentifier(): String {
            return systemIdentifier.toString()
        }

        override fun toString(): String {
            return "<!doctype ${getName()}>"
        }
    }

    public abstract class Tag(type: TokenType, public val treeBuilder: TreeBuilder) : Token(type) {
        internal var tagName: String? = null
        internal var normalName: String? = null // lc version of tag name, for case-insensitive tree build
        public var isSelfClosing: Boolean = false

        // start tags get attributes on construction. End tags get attributes on first new attribute (but only for parser convenience, not used).
        public var attributes: Attributes? = null
        private var attrName: String? = null // try to get attr names and vals in one shot, vs Builder
        private val attrNameSb: StringBuilder = StringBuilder()
        private var hasAttrName = false

        private var attrValue: String? = null
        private val attrValueSb: StringBuilder = StringBuilder()
        private var hasAttrValue = false
        private var hasEmptyAttrValue = false // distinguish boolean attribute from empty string value

        private val trackSource: Boolean = treeBuilder.trackSourceRange
        private var attrNameStart: Int = 0
        private var attrNameEnd: Int = 0
        private var attrValStart: Int = 0
        private var attrValEnd: Int = 0

        override fun reset(): Tag {
            super.reset()
            tagName = null
            normalName = null
            isSelfClosing = false
            attributes = null
            resetPendingAttr()
            return this
        }

        private fun resetPendingAttr() {
            reset(attrNameSb)
            attrName = null
            hasAttrName = false

            reset(attrValueSb)
            attrValue = null
            hasEmptyAttrValue = false
            hasAttrValue = false

            if (trackSource) {
                attrValEnd = Unset
                attrValStart = attrValEnd
                attrNameEnd = attrValStart
                attrNameStart = attrNameEnd
            }
        }

        public fun newAttribute() {
            if (attributes == null) attributes = Attributes()

            if (hasAttrName && attributes!!.size() < MaxAttributes) {
                // the tokeniser has skipped whitespace control chars, but trimming could collapse to empty for other control codes, so verify here
                var name = if (attrNameSb.isNotEmpty()) attrNameSb.toString() else attrName!!
                name = name.trim { it <= ' ' }
                if (name.isNotEmpty()) {
                    val value =
                        if (hasAttrValue) {
                            if (attrValueSb.isNotEmpty()) attrValueSb.toString() else attrValue
                        } else if (hasEmptyAttrValue) {
                            ""
                        } else {
                            null
                        }
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
                var attrRanges: MutableMap<String, Range.AttributeRange?>? =
                    attributes!!.userData(SharedConstants.AttrRangeKey) as MutableMap<String, Range.AttributeRange?>?
                if (attrRanges == null) {
                    attrRanges = mutableMapOf()
                    attributes!!.userData(SharedConstants.AttrRangeKey, attrRanges)
                }

                val normalizedName: String = if (!preserve) Normalizer.lowerCase(name) else name
                if (attrRanges.containsKey(
                        normalizedName,
                    )
                ) {
                    return // dedupe ranges as we go; actual attributes get deduped later for error count
                }

                // if there's no value (e.g. boolean), make it an implicit range at current
                if (!hasAttrValue) {
                    attrValEnd = attrNameEnd
                    attrValStart = attrValEnd
                }

                val range: Range.AttributeRange =
                    Range.AttributeRange(
                        Range(
                            Range.Position(attrNameStart, r.lineNumber(attrNameStart), r.columnNumber(attrNameStart)),
                            Range.Position(attrNameEnd, r.lineNumber(attrNameEnd), r.columnNumber(attrNameEnd)),
                        ),
                        Range(
                            Range.Position(attrValStart, r.lineNumber(attrValStart), r.columnNumber(attrValStart)),
                            Range.Position(attrValEnd, r.lineNumber(attrValEnd), r.columnNumber(attrValEnd)),
                        ),
                    )
                attrRanges[normalizedName] = range
            }
        }

        public fun hasAttributes(): Boolean {
            return attributes != null
        }

        /** Case-sensitive check  */
        public fun hasAttribute(key: String?): Boolean {
            return attributes != null && attributes!!.hasKey(key!!)
        }

        public fun hasAttributeIgnoreCase(key: String?): Boolean {
            return attributes != null && attributes!!.hasKeyIgnoreCase(key!!)
        }

        public fun finaliseTag() {
            // finalises for emit
            if (hasAttrName) {
                newAttribute()
            }
        }

        /** Preserves case  */
        public fun name(): String { // preserves case, for input into Tag.valueOf (which may drop case)
            Validate.isFalse(tagName == null || tagName!!.isEmpty())
            return tagName ?: ""
        }

        /** Lower case  */
        public fun retrieveNormalName(): String { // lower case, used in tree building for working out where in tree it should go
            return normalName ?: ""
        }

        public fun toStringName(): String {
            return if (tagName != null) tagName!! else "[unset]"
        }

        public fun name(name: String): Tag {
            tagName = name
            normalName = ParseSettings.normalName(tagName)
            return this
        }

        // these appenders are rarely hit in not null state-- caused by null chars.
        public fun appendTagName(append: String) {
            // might have null chars - need to replace with null replacement character
            val replacedAppend = append.replace(TokeniserState.nullChar, Tokeniser.ReplacementChar)
            tagName = if (tagName == null) replacedAppend else tagName + replacedAppend
            normalName = ParseSettings.normalName(tagName)
        }

        public fun appendTagName(append: Char) {
            appendTagName(append.toString())
        }

        public fun appendAttributeName(
            append: String,
            startPos: Int,
            endPos: Int,
        ) {
            // might have null chars because we eat in one pass - need to replace with null replacement character
            val resultAppend = append.replace(TokeniserState.nullChar, Tokeniser.ReplacementChar)

            ensureAttrName(startPos, endPos)
            if (attrNameSb.isEmpty()) {
                attrName = resultAppend
            } else {
                attrNameSb.append(resultAppend)
            }
        }

        public fun appendAttributeName(
            append: Char,
            startPos: Int,
            endPos: Int,
        ) {
            ensureAttrName(startPos, endPos)
            attrNameSb.append(append)
        }

        public fun appendAttributeValue(
            append: String?,
            startPos: Int,
            endPos: Int,
        ) {
            ensureAttrValue(startPos, endPos)
            if (attrValueSb.isEmpty()) {
                attrValue = append
            } else {
                attrValueSb.append(append)
            }
        }

        public fun appendAttributeValue(
            append: Char,
            startPos: Int,
            endPos: Int,
        ) {
            ensureAttrValue(startPos, endPos)
            attrValueSb.append(append)
        }

        public fun appendAttributeValue(
            appendCodepoints: IntArray,
            startPos: Int,
            endPos: Int,
        ) {
            ensureAttrValue(startPos, endPos)
            for (codepoint in appendCodepoints) {
                attrValueSb.appendCodePoint(codepoint)
            }
        }

        public fun setEmptyAttributeValue() {
            hasEmptyAttrValue = true
        }

        private fun ensureAttrName(
            startPos: Int,
            endPos: Int,
        ) {
            hasAttrName = true
            // if on second hit, we'll need to move to the builder
            if (attrName != null) {
                attrNameSb.append(attrName)
                attrName = null
            }
            if (trackSource) {
                attrNameStart = if (attrNameStart > Unset) attrNameStart else startPos // latches to first
                attrNameEnd = endPos
            }
        }

        private fun ensureAttrValue(
            startPos: Int,
            endPos: Int,
        ) {
            hasAttrValue = true
            // if on second hit, we'll need to move to the builder
            if (attrValue != null) {
                attrValueSb.append(attrValue)
                attrValue = null
            }
            if (trackSource) {
                attrValStart = if (attrValStart > Unset) attrValStart else startPos // latches to first
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

        public fun nameAttr(
            name: String?,
            attributes: Attributes?,
        ): StartTag {
            this.tagName = name
            this.attributes = attributes
            normalName = ParseSettings.normalName(tagName)
            return this
        }

        override fun toString(): String {
            val closer = if (isSelfClosing) "/>" else ">"
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
        private val data: StringBuilder = StringBuilder()
        private var dataS: String? = null // try to get in one shot
        public var bogus: Boolean = false

        override fun reset(): Token {
            super.reset()
            reset(data)
            dataS = null
            bogus = false
            return this
        }

        public fun getData(): String {
            return if (dataS != null) dataS!! else data.toString()
        }

        public fun append(append: String?): Comment {
            ensureData()
            if (data.isEmpty()) {
                dataS = append
            } else {
                data.append(append)
            }
            return this
        }

        public fun append(append: Char): Comment {
            ensureData()
            data.append(append)
            return this
        }

        private fun ensureData() {
            // if on second hit, we'll need to move to the builder
            if (dataS != null) {
                data.append(dataS)
                dataS = null
            }
        }

        override fun toString(): String {
            return "<!--${getData()}-->"
        }
    }

    public open class Character : Token(TokenType.Character), KCloneable<Character> {
        public var data: String? = null
            private set

        override fun reset(): Token {
            super.reset()
            data = null
            return this
        }

        public fun data(data: String?): Character {
            this.data = data
            return this
        }

        override fun toString(): String {
            return data.toString()
        }

        override fun clone(): Character {
            val character = Character()
            character.data = this.data
            return super.cloneCopy(character) as Character
        }
    }

    internal class CData(data: String?) : Character() {
        init {
            this.data(data)
        }

        override fun toString(): String {
            return "<![CDATA[$data]]>"
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

    public fun isEOF(): Boolean {
        return type == TokenType.EOF
    }

    public enum class TokenType {
        Doctype,
        StartTag,
        EndTag,
        Comment,
        Character, // note no CData - treated in builder as an extension of Character
        EOF,
    }

    public fun cloneCopy(token: Token): Token {
        token.type = this.type
        token._startPos = this._startPos
        token._endPos = this._endPos
        return token
    }

    public companion object {
        public const val Unset: Int = -1

        public fun reset(sb: StringBuilder?) {
            sb?.clear()
        }
    }
}
