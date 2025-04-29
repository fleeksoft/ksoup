package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.parser.ParseSettings
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.parser.Tag
import kotlin.jvm.JvmOverloads

/**
 * A TagSet controls the [Tag] configuration for a Document's parse, and its serialization. It contains the initial
 * defaults, and after the parse, any additionally discovered tags.
 *
 */
class TagSet {
    val tags: MutableMap<String, MutableMap<String, Tag>> = HashMap() // namespace -> tag name -> Tag

    val source: TagSet? // source to pull tags from on demand

    constructor() {
        source = null
    }

    constructor(original: TagSet) {
        this.source = original
    }

    /**
     * Insert a tag into this TagSet. If the tag already exists, it is replaced.
     *
     * Tags explicitly added like this are considered to be known tags (vs those that are dynamically created via
     * .valueOf() if not already in the set.
     *
     * @param tag the tag to add
     * @return this TagSet
     */
    fun add(tag: Tag): TagSet {
        tag.set(Tag.Known)
        doAdd(tag)
        return this
    }

    private fun doAdd(tag: Tag) {
        tags.getOrPut(tag.namespace()) { HashMap() }.put(tag.tagName, tag)
    }


    /**
     * Get an existing Tag from this TagSet by tagName and namespace. The tag name is not normalized, to support mixed
     * instances.
     *
     * @param tagName the case-sensitive tag name
     * @param namespace the namespace
     * @return the tag, or null if not found
     */
    fun get(tagName: String, namespace: String): Tag? {

        // get from our tags
        tags[namespace]?.let { nsTags ->
            nsTags[tagName]?.let { return it }
        }

        // not found; clone on demand from source if exists
        source?.get(tagName, namespace)?.let { tag ->
            val copy = tag.clone()
            doAdd(copy)
            return copy
        }


        return null
    }

    /** Tag.valueOf with the normalName via the token.normalName, to save redundant lower-casing passes.  */
    fun valueOf(tagName: String, normalName: String, namespace: String, preserveTagCase: Boolean): Tag {
        var tName = tagName.trim()
        Validate.notEmpty(tName)
        var tag = get(tName, namespace)
        if (tag != null) return tag

        // not found by tagName, try by normal
        tName = if (preserveTagCase) tName else normalName
        tag = get(normalName, namespace)
        if (tag != null) {
            if (preserveTagCase && tName != normalName) {
                tag = tag.clone() // copy so that the name update doesn't reset all instances
                tag.tagName = tName
                doAdd(tag)
            }
            return tag
        }

        // not defined: return a new one
        tag = Tag(tName, normalName, namespace)
        doAdd(tag)
        return tag
    }

    /**
     * Get a Tag by name from this TagSet. If not previously defined (unknown), returns a new tag.
     *
     * New tags will be added to this TagSet.
     *
     * @param tagName Name of tag, e.g. "p".
     * @param namespace the namespace for the tag.
     * @param settings used to control tag name sensitivity
     * @return The tag, either defined or new generic.
     */
    /**
     * Get a Tag by name from this TagSet. If not previously defined (unknown), returns a new tag.
     *
     * New tags will be added to this TagSet.
     *
     * @param tagName Name of tag, e.g. "p". **Case-sensitive**.
     * @param namespace the namespace for the tag.
     * @return The tag, either defined or new generic.
     * @see .valueOf
     */
    @JvmOverloads
    fun valueOf(
        tagName: String,
        namespace: String,
        settings: ParseSettings = ParseSettings.preserveCase
    ): Tag {
        return valueOf(tagName, ParseSettings.normalName(tagName), namespace, settings.preserveTagCase())
    }

    override fun equals(o: Any?): Boolean {
        if (o !is TagSet) return false
        val tagSet = o
        return tags == tagSet.tags
    }

    override fun hashCode(): Int {
        return tags.hashCode()
    }

    private fun setupTags(namespace: String, tagNames: Array<String>, tagModifier: (Tag) -> Unit): TagSet {
        for (tagName in tagNames) {
            var tag = get(tagName, namespace)
            if (tag == null) {
                tag = Tag(tagName, tagName, namespace) // normal name is already normal here
                tag.options = 0 // clear defaults
                add(tag)
            }
            tagModifier(tag)
        }
        return this
    }

    companion object {
        val HtmlTagSet: TagSet by lazy { initHtmlDefault() }

        /**
         * Returns a mutable copy of the default HTML tag set.
         */
        fun Html(): TagSet {
            return TagSet(HtmlTagSet)
        }

        // Default HTML initialization
        /**
         * Initialize the default HTML tag set.
         */
        fun initHtmlDefault(): TagSet {
            val blockTags = arrayOf<String>(
                "html", "head", "body", "frameset", "script", "noscript", "style", "meta", "link", "title", "frame",
                "noframes", "section", "nav", "aside", "hgroup", "header", "footer", "p", "h1", "h2", "h3", "h4", "h5",
                "h6", "br", "button",
                "ul", "ol", "pre", "div", "blockquote", "hr", "address", "figure", "figcaption", "form", "fieldset", "ins",
                "del", "dl", "dt", "dd", "li", "table", "caption", "thead", "tfoot", "tbody", "colgroup", "col", "tr", "th",
                "td", "video", "audio", "canvas", "details", "menu", "plaintext", "template", "article", "main",
                "center", "template",
                "dir", "applet", "marquee", "listing",  // deprecated but still known / special handling
                "#root" // the outer Document
            )
            val inlineTags = arrayOf<String>(
                "object", "base", "font", "tt", "i", "b", "u", "big", "small", "em", "strong", "dfn", "code", "samp", "kbd",
                "var", "cite", "abbr", "time", "acronym", "mark", "ruby", "rt", "rp", "rtc", "a", "img", "wbr", "map",
                "q",
                "sub", "sup", "bdo", "iframe", "embed", "span", "input", "select", "textarea", "label", "optgroup",
                "option", "legend", "datalist", "keygen", "output", "progress", "meter", "area", "param", "source", "track",
                "summary", "command", "device", "area", "basefont", "bgsound", "menuitem", "param", "source", "track",
                "data", "bdi", "s", "strike", "nobr",
                "rb",  // deprecated but still known / special handling
            )
            val inlineContainers = arrayOf<String>( // can only contain inline; aka phrasing content
                "title", "a", "p", "h1", "h2", "h3", "h4", "h5", "h6", "pre", "address", "li", "th", "td", "script", "style",
                "ins", "del", "s", "button"
            )
            val voidTags = arrayOf<String>(
                "meta", "link", "base", "frame", "img", "br", "wbr", "embed", "hr", "input", "keygen", "col", "command",
                "device", "area", "basefont", "bgsound", "menuitem", "param", "source", "track"
            )
            val preserveWhitespaceTags = arrayOf<String>(
                "pre", "plaintext", "title", "textarea", "script"
            )
            val rcdataTags = arrayOf<String>("title", "textarea")
            val dataTags = arrayOf<String>("iframe", "noembed", "noframes", "script", "style", "xmp")
            val formSubmitTags: Array<String> = SharedConstants.FormSubmitTags
            val blockMathTags = arrayOf<String>("math")
            val inlineMathTags = arrayOf<String>("mi", "mo", "msup", "mn", "mtext")
            val blockSvgTags = arrayOf<String>("svg", "femerge", "femergenode") // note these are LC versions, but actually preserve case
            val inlineSvgTags = arrayOf<String>("text")

            return TagSet()
                .setupTags(Parser.NamespaceHtml, blockTags) { it.set(Tag.Block) }
                .setupTags(Parser.NamespaceHtml, inlineTags) { it.set(0) }
                .setupTags(Parser.NamespaceHtml, inlineContainers) { it.set(Tag.InlineContainer) }
                .setupTags(Parser.NamespaceHtml, voidTags) { it.set(Tag.Void) }
                .setupTags(Parser.NamespaceHtml, preserveWhitespaceTags) { it.set(Tag.PreserveWhitespace) }
                .setupTags(Parser.NamespaceHtml, rcdataTags) { it.set(Tag.RcData) }
                .setupTags(Parser.NamespaceHtml, dataTags) { it.set(Tag.Data) }
                .setupTags(Parser.NamespaceHtml, formSubmitTags) { it.set(Tag.FormSubmittable) }
                .setupTags(Parser.NamespaceMathml, blockMathTags) { it.set(Tag.Block) }
                .setupTags(Parser.NamespaceMathml, inlineMathTags) { it.set(0) }
                .setupTags(Parser.NamespaceSvg, blockSvgTags) { it.set(Tag.Block) }
                .setupTags(Parser.NamespaceSvg, inlineSvgTags) { it.set(0) }

        }
    }
}