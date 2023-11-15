package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.Normalizer
import com.fleeksoft.ksoup.ported.CloneNotSupportedException
import com.fleeksoft.ksoup.ported.Cloneable
import com.fleeksoft.ksoup.ported.Consumer
import kotlin.jvm.JvmOverloads

/**
 * Tag capabilities.
 *
 * @author Sabeeh, fleeksoft@gmail.com
 */
data class Tag(
    /**
     * Get this tag's name.
     *
     * @return the tag's name
     */
    var name: String,
    private var namespace: String,
) : Cloneable<Tag> {
    private val normalName: String = Normalizer.lowerCase(name)

    /**
     * Gets if this is a block tag.
     *
     * @return if block tag
     */
    var isBlock = true // block
        private set
    private var formatAsBlock = true // should be formatted as a block

    /**
     * Get if this is an empty tag
     *
     * @return if this is an empty tag
     */
    var isEmpty = false // can hold nothing; e.g. img
        private set
    private var selfClosing =
        false // can self close (<foo />). used for unknown tags that self close, without forcing them as empty.
    private var preserveWhitespace = false // for pre, textarea, script etc

    /**
     * Get if this tag represents a control associated with a form. E.g. input, textarea, output
     * @return if associated with a form
     */
    var isFormListed = false // a control that appears in forms: input, textarea, output etc
        private set

    /**
     * Get if this tag represents an element that should be submitted with a form. E.g. input, option
     * @return if submittable with a form
     */
    var isFormSubmittable = false // a control that can be submitted in a form: input etc
        private set

    /**
     * Get this tag's normalized (lowercased) name.
     * @return the tag's normal name.
     */
    fun normalName(): String {
        return normalName
    }

    fun namespace(): String {
        return namespace
    }

    /**
     * Gets if this tag should be formatted as a block (or as inline)
     *
     * @return if should be formatted as block or inline
     */
    fun formatAsBlock(): Boolean {
        return formatAsBlock
    }

    fun isInline(): Boolean = !isBlock

    /**
     * Get if this tag is self-closing.
     *
     * @return if this tag should be output as self-closing.
     */
    fun isSelfClosing(): Boolean {
        return isEmpty || selfClosing
    }

    fun isKnownTag(): Boolean = Tags.containsKey(name)

    /**
     * Get if this tag should preserve whitespace within child text nodes.
     *
     * @return if preserve whitespace
     */
    fun preserveWhitespace(): Boolean {
        return preserveWhitespace
    }

    fun setSelfClosing(): Tag {
        selfClosing = true
        return this
    }

    override fun toString(): String {
        return name
    }

    override fun clone(): Tag {
        val clone = this.copy()
        clone.isBlock = this.isBlock
        clone.formatAsBlock = this.formatAsBlock
        clone.isEmpty = this.isEmpty
        clone.isFormListed = this.isFormListed
        clone.isFormSubmittable = this.isFormSubmittable
        clone.selfClosing = this.selfClosing
        clone.preserveWhitespace = this.preserveWhitespace
        return clone
    }

    companion object {
        private val Tags: MutableMap<String, Tag> = HashMap<String, Tag>() // map of known tags


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
        fun valueOf(
            tagName: String,
            namespace: String = Parser.NamespaceHtml,
            settings: ParseSettings? = ParseSettings.preserveCase,
        ): Tag {
            var tagName = tagName
            Validate.notEmpty(tagName)
            var tag = Tags[tagName]
            if (tag != null && tag.namespace == namespace) return tag
            tagName = settings!!.normalizeTag(tagName) // the name we'll use
            Validate.notEmpty(tagName)
            val normalName: String =
                Normalizer.lowerCase(tagName) // the lower-case name to get tag settings off
            tag = Tags[normalName]
            if (tag != null && tag.namespace == namespace) {
                if (settings!!.preserveTagCase() && tagName != normalName) {
                    tag =
                        tag.clone() // get a new version vs the static one, so name update doesn't reset all
                    tag.name = tagName
                }
                return tag
            }

            // not defined: create default; go anywhere, do anything! (incl be inside a <p>)
            tag = Tag(tagName, namespace)
            tag.isBlock = false
            return tag
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
        fun valueOf(tagName: String, settings: ParseSettings?): Tag? {
            return valueOf(tagName, Parser.NamespaceHtml, settings)
        }

        /**
         * Check if this tagname is a known tag.
         *
         * @param tagName name of tag
         * @return if known HTML tag
         */
        fun isKnownTag(tagName: String): Boolean {
            return Tags.containsKey(tagName)
        }

        // internal static initialisers:
        // prepped from http://www.w3.org/TR/REC-html40/sgml/dtd.html and other sources
        private val blockTags = arrayOf(
            "html",
            "head",
            "body",
            "frameset",
            "script",
            "noscript",
            "style",
            "meta",
            "link",
            "title",
            "frame",
            "noframes",
            "section",
            "nav",
            "aside",
            "hgroup",
            "header",
            "footer",
            "p",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "ul",
            "ol",
            "pre",
            "div",
            "blockquote",
            "hr",
            "address",
            "figure",
            "figcaption",
            "form",
            "fieldset",
            "ins",
            "del",
            "dl",
            "dt",
            "dd",
            "li",
            "table",
            "caption",
            "thead",
            "tfoot",
            "tbody",
            "colgroup",
            "col",
            "tr",
            "th",
            "td",
            "video",
            "audio",
            "canvas",
            "details",
            "menu",
            "plaintext",
            "template",
            "article",
            "main",
            "svg",
            "math",
            "center",
            "template",
            "dir",
            "applet",
            "marquee",
            "listing", // deprecated but still known / special handling
        )
        private val inlineTags = arrayOf(
            "object",
            "base",
            "font",
            "tt",
            "i",
            "b",
            "u",
            "big",
            "small",
            "em",
            "strong",
            "dfn",
            "code",
            "samp",
            "kbd",
            "var",
            "cite",
            "abbr",
            "time",
            "acronym",
            "mark",
            "ruby",
            "rt",
            "rp",
            "rtc",
            "a",
            "img",
            "br",
            "wbr",
            "map",
            "q",
            "sub",
            "sup",
            "bdo",
            "iframe",
            "embed",
            "span",
            "input",
            "select",
            "textarea",
            "label",
            "button",
            "optgroup",
            "option",
            "legend",
            "datalist",
            "keygen",
            "output",
            "progress",
            "meter",
            "area",
            "param",
            "source",
            "track",
            "summary",
            "command",
            "device",
            "area",
            "basefont",
            "bgsound",
            "menuitem",
            "param",
            "source",
            "track",
            "data",
            "bdi",
            "s",
            "strike",
            "nobr",
            "rb", // deprecated but still known / special handling
            "text", // in SVG NS
            "mi",
            "mo",
            "msup",
            "mn",
            "mtext", // in MathML NS, to ensure inline
        )
        private val emptyTags = arrayOf(
            "meta",
            "link",
            "base",
            "frame",
            "img",
            "br",
            "wbr",
            "embed",
            "hr",
            "input",
            "keygen",
            "col",
            "command",
            "device",
            "area",
            "basefont",
            "bgsound",
            "menuitem",
            "param",
            "source",
            "track",
        )

        // todo - rework this to format contents as inline; and update html emitter in Element. Same output, just neater.
        private val formatAsInlineTags = arrayOf(
            "title",
            "a",
            "p",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "pre",
            "address",
            "li",
            "th",
            "td",
            "script",
            "style",
            "ins",
            "del",
            "s",
        )
        private val preserveWhitespaceTags = arrayOf(
            "pre",
            "plaintext",
            "title",
            "textarea", // script is not here as it is a data node, which always preserve whitespace
        )

        // todo: I think we just need submit tags, and can scrub listed
        private val formListedTags = arrayOf(
            "button",
            "fieldset",
            "input",
            "keygen",
            "object",
            "output",
            "select",
            "textarea",
        )
        private val formSubmitTags = arrayOf(
            "input",
            "keygen",
            "object",
            "select",
            "textarea",
        )
        private val namespaces: MutableMap<String, Array<String>> = HashMap<String, Array<String>>()

        init {
            namespaces[Parser.NamespaceMathml] =
                arrayOf<String>("math", "mi", "mo", "msup", "mn", "mtext")
            namespaces[Parser.NamespaceSvg] = arrayOf<String>("svg", "text")
            // We don't need absolute coverage here as other cases will be inferred by the HtmlTreeBuilder
        }

        private fun setupTags(
            tagNames: Array<String>,
            tagModifier: Consumer<Tag>,
        ) {
            for (tagName in tagNames) {
                var tag = Tags[tagName]
                if (tag == null) {
                    tag = Tag(tagName, Parser.NamespaceHtml)
                    Tags[tag.name] = tag
                }
                tagModifier.accept(tag)
            }
        }

        init {
            setupTags(
                blockTags,
                Consumer<Tag> { tag: Tag ->
                    tag.isBlock = true
                    tag.formatAsBlock = true
                },
            )
            setupTags(
                inlineTags,
                Consumer<Tag> { tag: Tag ->
                    tag.isBlock = false
                    tag.formatAsBlock = false
                },
            )
            setupTags(
                emptyTags,
                Consumer<Tag> { tag: Tag -> tag.isEmpty = true },
            )
            setupTags(
                formatAsInlineTags,
                Consumer<Tag> { tag: Tag -> tag.formatAsBlock = false },
            )
            setupTags(
                preserveWhitespaceTags,
                Consumer<Tag> { tag: Tag -> tag.preserveWhitespace = true },
            )
            setupTags(
                formListedTags,
                Consumer<Tag> { tag: Tag -> tag.isFormListed = true },
            )
            setupTags(
                formSubmitTags,
                Consumer<Tag> { tag: Tag -> tag.isFormSubmittable = true },
            )
            for ((key, value) in namespaces) {
                setupTags(
                    value,
                    Consumer<Tag> { tag: Tag -> tag.namespace = key },
                )
            }
        }
    }
}
