/*
 * Kotlin port of jsoup's Tag.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.nodes.TagSet
import com.fleeksoft.ksoup.ported.KCloneable
import kotlin.js.JsName
import kotlin.jvm.JvmOverloads

/**
 * Tag capabilities.
 *
 */
public data class Tag(var tagName: String, var normalName: String, var namespace: String) : KCloneable<Tag> {
    var options: Int = 0

    constructor(tagName: String) : this(tagName, ParseSettings.normalName(tagName), Parser.NamespaceHtml)
    constructor(tagName: String, namespace: String) : this(tagName, ParseSettings.normalName(tagName), namespace)

    /**
     * Get this tag's name.
     * @return the tag's name
     */
    fun name(): String {
        return tagName
    }

    override fun hashCode(): Int {
        return arrayOf(tagName, namespace).contentHashCode()
    }

    /**
     * Change the tag's name. As Tags are reused throughout a Document, this will change the name for all uses of this tag.
     * @param tagName the new name of the tag. Case-sensitive.
     * @return this tag
     */
    fun name(tagName: String): Tag {
        this.tagName = tagName
        this.normalName = ParseSettings.normalName(tagName)
        return this
    }

    /**
     * Get this tag's prefix, if it has one; else the empty string.
     *
     * For example, `<book:title>` has prefix `book`, and tag name `book:title`.
     * @return the tag's prefix
     */
    fun prefix(): String {
        val pos = tagName.indexOf(':')
        return if (pos == -1) "" else tagName.substring(0, pos)
    }

    /**
     * Get this tag's local name. The local name is the name without the prefix (if any).
     *
     * For example, `<book:title>` has local name `title`, and tag name `book:title`.
     * @return the tag's local name
     * @since 1.20.1
     */
    fun localName(): String {
        val pos = tagName.indexOf(':')
        return if (pos == -1) tagName else tagName.substring(pos + 1)
    }

    /**
     * Gets if this is a block tag.
     *
     * @return if block tag
     */
    fun isBlock(): Boolean = (options and Block) != 0

    /**
     * Get if this is a void (aka empty) tag.
     *
     * @return true if this is a void tag
     */
    fun isEmpty(): Boolean = (options and Void) != 0

    /**
     * Get if this tag represents an element that should be submitted with a form. E.g. input, option
     * @return if submittable with a form
     */
    fun isFormSubmittable(): Boolean {
        return (options and FormSubmittable) != 0
    }

    fun setSeenSelfClose() {
        options = options or SeenSelfClose // does not change known status
    }

    /**
     * If this Tag uses a specific text TokeniserState for its content, returns that; otherwise null.
     */
    fun textState(): TokeniserState? {
        return if (`is`(RcData)) TokeniserState.Rcdata
        else if (`is`(Data)) TokeniserState.Rawtext
        else null
    }

    /**
     * Get this tag's normalized (lowercased) name.
     * @return the tag's normal name.
     */
    @JsName("getNormalName")
    public fun normalName(): String {
        return normalName
    }

    /**
     * Get this tag's namespace.
     * @return the tag's namespace
     */
    @JsName("getNamespace")
    public fun namespace(): String {
        return namespace
    }

    /**
     * Set the tag's namespace. As Tags are reused throughout a Document, this will change the namespace for all uses of this tag.
     * @param namespace the new namespace of the tag.
     * @return this tag
     */
    fun namespace(namespace: String): Tag {
        this.namespace = namespace
        return this
    }

    /**
     * Set an option on this tag.
     *
     * Once a tag has a setting applied, it will be considered a known tag.
     * @param option the option to set
     * @return this tag
     */
    fun set(option: Int): Tag {
        options = options or option
        options = options or Known // considered known if touched
        return this
    }

    /**
     * Test if an option is set on this tag.
     *
     * @param option the option to test
     * @return true if the option is set
     */
    fun `is`(option: Int): Boolean {
        return (options and option) != 0
    }

    /**
     * Clear (unset) an option from this tag.
     * @param option the option to clear
     * @return this tag
     */
    fun clear(option: Int): Tag {
        options = options and option.inv()
        // considered known if touched, unless explicitly clearing known
        if (option != Known) options = options or Known
        return this
    }

    /**
     * Get if this is an InlineContainer tag.
     *
     * @return true if an InlineContainer (which formats children as inline).
     */
    @Deprecated("setting is only used within the Printer. Will be removed")
    public fun formatAsBlock(): Boolean {
        return (options and InlineContainer) != 0
    }

    /*
    * Gets if this tag is an inline tag. Just the opposite of isBlock.
    * @return if this tag is an inline tag.
    */
    public fun isInline(): Boolean = (options and Block) == 0

    /**
     * Get if this tag is self-closing.
     *
     * @return if this tag should be output as self-closing.
     */
    public fun isSelfClosing(): Boolean {
        return (options and SelfClose) != 0 || (options and Void) != 0
    }


    /**
     * Get if this is a pre-defined tag in the TagSet, or was auto created on parsing.
     *
     * @return true if the tag is a known tag, false otherwise
     */
    public fun isKnownTag(): Boolean = (options and Known) != 0

    /**
     * Get if this tag should preserve whitespace within child text nodes.
     *
     * @return if preserve whitespace
     */
    public fun preserveWhitespace(): Boolean {
        return (options and PreserveWhitespace) != 0
    }

    override fun toString(): String {
        return tagName
    }

    override fun clone(): Tag {
        val clone = this.copy()
        clone.options = this.options
        return clone
    }

    public companion object {

        /** Tag option: the tag is known (specifically defined). */
        const val Known = 1

        /** Tag option: the tag is a void tag (e.g. <img>). */
        const val Void = 1 shl 1

        /** Tag option: the tag is a block tag (e.g. <div>, <p>). */
        const val Block = 1 shl 2

        /** Tag option: block tag only holding inline tags (e.g. <p>); must also set Block. */
        const val InlineContainer = 1 shl 3

        /** Tag option: the tag can self-close (e.g. <foo />). */
        const val SelfClose = 1 shl 4

        /** Tag option: the tag has been seen self-closing in this parse. */
        const val SeenSelfClose = 1 shl 5

        /** Tag option: the tag preserves whitespace (e.g. <pre>). */
        const val PreserveWhitespace = 1 shl 6

        /** Tag option: the tag is an RCDATA element (e.g. <title>, <textarea>). */
        const val RcData = 1 shl 7

        /** Tag option: the tag is a Data element (e.g. <style>, <script>). */
        const val Data = 1 shl 8

        /** Tag option: submit value when form submitted (e.g. <input>). */
        const val FormSubmittable = 1 shl 9

        /**
         * Get a Tag by name. If not previously defined (unknown), returns a new generic tag, that can do anything.
         *
         *
         * Pre-defined tags (p, div etc) will be ==, but unknown tags are not registered and will only .equals().
         *
         *
         * @param tagName Name of tag, e.g. "p". Case-insensitive.
         * @param namespace the namespace for the tag.
         * @param settings used to control tag name sensitivity
         * @return The tag, either defined or new generic.
         */

        /**
         * Get a Tag by name. If not previously defined (unknown), returns a new generic tag, that can do anything.
         *
         *
         * Pre-defined tags (P, DIV etc) will be ==, but unknown tags are not registered and will only .equals().
         *
         *
         * @param tagName Name of tag, e.g. "p". **Case sensitive**.
         * @return The tag, either defined or new generic.
         * @see .valueOf
         */
        @JvmOverloads
        public fun valueOf(tagName: String, namespace: String = Parser.NamespaceHtml, settings: ParseSettings = ParseSettings.preserveCase): Tag {
            return TagSet.Html().valueOf(tagName, ParseSettings.normalName(tagName), namespace, settings.preserveTagCase())
        }

        /**
         * Get a Tag by name. If not previously defined (unknown), returns a new generic tag, that can do anything.
         *
         *
         * Pre-defined tags (P, DIV etc) will be ==, but unknown tags are not registered and will only .equals().
         *
         *
         * @param tagName Name of tag, e.g. "p". **Case sensitive**.
         * @param settings used to control tag name sensitivity
         * @return The tag, either defined or new generic.
         * @see .valueOf
         */
        public fun valueOf(tagName: String, settings: ParseSettings): Tag {
            return valueOf(tagName, Parser.NamespaceHtml, settings)
        }

        /**
         * Check if this tagname is a known tag.
         *
         * @param tagName name of tag
         * @return if known HTML tag
         */
        public fun isKnownTag(tagName: String): Boolean {
            return TagSet.HtmlTagSet.get(tagName, Parser.NamespaceHtml) != null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Tag

        if (options != other.options) return false
        if (tagName != other.tagName) return false
        if (normalName != other.normalName) return false
        if (namespace != other.namespace) return false

        return true
    }
}
