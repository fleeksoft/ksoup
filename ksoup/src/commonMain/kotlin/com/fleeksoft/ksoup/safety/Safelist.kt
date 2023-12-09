package com.fleeksoft.ksoup.safety
/*
    Thank you to Ryan Grove (wonko.com) for the Ruby HTML cleaner http://github.com/rgrove/sanitize/, which inspired
    this safe-list configuration, and the initial defaults.
 */
import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.Normalizer.lowerCase
import com.fleeksoft.ksoup.nodes.Attribute
import com.fleeksoft.ksoup.nodes.Attributes
import com.fleeksoft.ksoup.nodes.Element

/**
Safe-lists define what HTML (elements and attributes) to allow through the cleaner. Everything else is removed.
<p>
Start with one of the defaults:
</p>
<ul>
<li>{@link #none}
<li>{@link #simpleText}
<li>{@link #basic}
<li>{@link #basicWithImages}
<li>{@link #relaxed}
</ul>
<p>
If you need to allow more through (please be careful!), tweak a base safelist with:
</p>
<ul>
<li>{@link #addTags(String... tagNames)}
<li>{@link #addAttributes(String tagName, String... attributes)}
<li>{@link #addEnforcedAttribute(String tagName, String attribute, String value)}
<li>{@link #addProtocols(String tagName, String attribute, String... protocols)}
</ul>
<p>
You can remove any setting from an existing safelist with:
</p>
<ul>
<li>{@link #removeTags(String... tagNames)}
<li>{@link #removeAttributes(String tagName, String... attributes)}
<li>{@link #removeEnforcedAttribute(String tagName, String attribute)}
<li>{@link #removeProtocols(String tagName, String attribute, String... removeProtocols)}
</ul>

<p>
The cleaner and these safelists assume that you want to clean a <code>body</code> fragment of HTML (to add user
supplied HTML into a templated page), and not to clean a full HTML document. If the latter is the case, you could wrap
the templated document HTML around the cleaned body HTML.
</p>
<p>
If you are going to extend a safelist, please be very careful. Make sure you understand what attributes may lead to
XSS attack vectors. URL attributes are particularly vulnerable and require careful validation. See
the <a href="https://owasp.org/www-community/xss-filter-evasion-cheatsheet">XSS Filter Evasion Cheat Sheet</a> for some
XSS attack examples (that jsoup will safegaurd against the default Cleaner and Safelist configuration).
</p>
 */
public open class Safelist() {
    private val tagNames: MutableSet<TagName>
    private val attributes: MutableMap<TagName, MutableSet<AttributeKey>> // tag -> attribute[]. allowed attributes [href] for a tag.
    private val enforcedAttributes: MutableMap<TagName, MutableMap<AttributeKey, AttributeValue>>
    private val protocols: MutableMap<TagName, MutableMap<AttributeKey, MutableSet<Protocol>>>
    private var preserveRelativeLinks = // option to preserve relative links
        false

    /**
     * Create a new, empty safelist. Generally it will be better to start with a default prepared safelist instead.
     *
     * @see .basic
     * @see .basicWithImages
     * @see .simpleText
     * @see .relaxed
     */
    init {
        tagNames = HashSet<TagName>()
        attributes = mutableMapOf()
        enforcedAttributes = mutableMapOf()
        protocols = mutableMapOf()
    }

    /**
     * Deep copy an existing Safelist to a new Safelist.
     * @param copy the Safelist to copy
     */
    public constructor(copy: Safelist) : this() {
        tagNames.addAll(copy.tagNames)
        for ((key, value) in copy.attributes) {
            attributes[key] = HashSet<AttributeKey>(value)
        }
        for ((key, value) in copy.enforcedAttributes) {
            enforcedAttributes[key] =
                HashMap<AttributeKey, AttributeValue>(
                    value,
                )
        }
        for ((key, value) in copy.protocols) {
            val attributeProtocolsCopy: MutableMap<AttributeKey, MutableSet<Protocol>> =
                mutableMapOf()
            for ((key1, value1) in value) {
                attributeProtocolsCopy[key1] =
                    HashSet<Protocol>(
                        value1,
                    )
            }
            protocols[key] = attributeProtocolsCopy
        }
        preserveRelativeLinks = copy.preserveRelativeLinks
    }

    /**
     * Add a list of allowed elements to a safelist. (If a tag is not allowed, it will be removed from the HTML.)
     *
     * @param tags tag names to allow
     * @return this (for chaining)
     */
    public fun addTags(vararg tags: String): Safelist {
        for (tagName in tags) {
            Validate.notEmpty(tagName)
            Validate.isFalse(
                tagName.equals("noscript", ignoreCase = true),
                "noscript is unsupported in Safelists, due to incompatibilities between parsers with and without script-mode enabled",
            )
            tagNames.add(TagName.valueOf(tagName))
        }
        return this
    }

    /**
     * Remove a list of allowed elements from a safelist. (If a tag is not allowed, it will be removed from the HTML.)
     *
     * @param tags tag names to disallow
     * @return this (for chaining)
     */
    public fun removeTags(vararg tags: String): Safelist {
        for (tag in tags) {
            Validate.notEmpty(tag)
            val tagName = TagName.valueOf(tag)
            if (tagNames.remove(tagName)) { // Only look in sub-maps if tag was allowed
                attributes.remove(tagName)
                enforcedAttributes.remove(tagName)
                protocols.remove(tagName)
            }
        }
        return this
    }

    /**
     * Add a list of allowed attributes to a tag. (If an attribute is not allowed on an element, it will be removed.)
     *
     *
     * E.g.: `addAttributes("a", "href", "class")` allows `href` and `class` attributes
     * on `a` tags.
     *
     *
     *
     * To make an attribute valid for **all tags**, use the pseudo tag `:all`, e.g.
     * `addAttributes(":all", "class")`.
     *
     *
     * @param tag  The tag the attributes are for. The tag will be added to the allowed tag list if necessary.
     * @param attributes List of valid attributes for the tag
     * @return this (for chaining)
     */
    public fun addAttributes(
        tag: String,
        vararg attributes: String,
    ): Safelist {
        Validate.notEmpty(tag)
        Validate.isTrue(attributes.isNotEmpty(), "No attribute names supplied.")
        val tagName = TagName.valueOf(tag)
        tagNames.add(tagName)
        val attributeSet: MutableSet<AttributeKey> = HashSet<AttributeKey>()
        for (key in attributes) {
            Validate.notEmpty(key)
            attributeSet.add(AttributeKey.valueOf(key))
        }
        if (this.attributes.containsKey(tagName)) {
            val currentSet: MutableSet<AttributeKey> = this.attributes[tagName]!!
            currentSet.addAll(attributeSet)
        } else {
            this.attributes[tagName] = attributeSet
        }
        return this
    }

    /**
     * Remove a list of allowed attributes from a tag. (If an attribute is not allowed on an element, it will be removed.)
     *
     *
     * E.g.: `removeAttributes("a", "href", "class")` disallows `href` and `class`
     * attributes on `a` tags.
     *
     *
     *
     * To make an attribute invalid for **all tags**, use the pseudo tag `:all`, e.g.
     * `removeAttributes(":all", "class")`.
     *
     *
     * @param tag  The tag the attributes are for.
     * @param attributes List of invalid attributes for the tag
     * @return this (for chaining)
     */
    public fun removeAttributes(
        tag: String,
        vararg attributes: String,
    ): Safelist {
        Validate.notEmpty(tag)
        Validate.isTrue(attributes.isNotEmpty(), "No attribute names supplied.")
        val tagName = TagName.valueOf(tag)
        val attributeSet: MutableSet<AttributeKey> = HashSet<AttributeKey>()
        for (key in attributes) {
            Validate.notEmpty(key)
            attributeSet.add(AttributeKey.valueOf(key))
        }
        if (tagNames.contains(tagName) && this.attributes.containsKey(tagName)) { // Only look in sub-maps if tag was allowed
            val currentSet: MutableSet<AttributeKey> = this.attributes[tagName]!!
            currentSet.removeAll(attributeSet)
            if (currentSet.isEmpty()) {
                // Remove tag from attribute map if no attributes are allowed for tag
                this.attributes.remove(tagName)
            }
        }
        if (tag == All) { // Attribute needs to be removed from all individually set tags
            val it: MutableIterator<Map.Entry<TagName, MutableSet<AttributeKey>>> =
                this.attributes.entries.iterator()
            while (it.hasNext()) {
                val (_, currentSet) = it.next()
                currentSet.removeAll(attributeSet)
                if (currentSet.isEmpty()) {
                    // Remove tag from attribute map if no attributes are allowed for tag
                    it.remove()
                }
            }
        }
        return this
    }

    /**
     * Add an enforced attribute to a tag. An enforced attribute will always be added to the element. If the element
     * already has the attribute set, it will be overridden with this value.
     *
     *
     * E.g.: `addEnforcedAttribute("a", "rel", "nofollow")` will make all `a` tags output as
     * `<a href="..." rel="nofollow">`
     *
     *
     * @param tag   The tag the enforced attribute is for. The tag will be added to the allowed tag list if necessary.
     * @param attribute   The attribute name
     * @param value The enforced attribute value
     * @return this (for chaining)
     */
    public fun addEnforcedAttribute(
        tag: String,
        attribute: String,
        value: String,
    ): Safelist {
        Validate.notEmpty(tag)
        Validate.notEmpty(attribute)
        Validate.notEmpty(value)
        val tagName = TagName.valueOf(tag)
        tagNames.add(tagName)
        val attrKey = AttributeKey.valueOf(attribute)
        val attrVal = AttributeValue.valueOf(value)
        if (enforcedAttributes.containsKey(tagName)) {
            enforcedAttributes[tagName]?.set(attrKey, attrVal)
        } else {
            val attrMap: MutableMap<AttributeKey, AttributeValue> =
                HashMap<AttributeKey, AttributeValue>()
            attrMap[attrKey] = attrVal
            enforcedAttributes[tagName] = attrMap
        }
        return this
    }

    /**
     * Remove a previously configured enforced attribute from a tag.
     *
     * @param tag   The tag the enforced attribute is for.
     * @param attribute   The attribute name
     * @return this (for chaining)
     */
    public fun removeEnforcedAttribute(
        tag: String,
        attribute: String,
    ): Safelist {
        Validate.notEmpty(tag)
        Validate.notEmpty(attribute)
        val tagName = TagName.valueOf(tag)
        if (tagNames.contains(tagName) && enforcedAttributes.containsKey(tagName)) {
            val attrKey = AttributeKey.valueOf(attribute)
            val attrMap: MutableMap<AttributeKey, AttributeValue>? = enforcedAttributes[tagName]
            attrMap?.remove(attrKey)
            if (attrMap?.isEmpty() == true) {
                // Remove tag from enforced attribute map if no enforced attributes are present
                enforcedAttributes.remove(tagName)
            }
        }
        return this
    }

    /**
     * Configure this Safelist to preserve relative links in an element's URL attribute, or convert them to absolute
     * links. By default, this is **false**: URLs will be  made absolute (e.g. start with an allowed protocol, like
     * e.g. `http://`.
     *
     *
     * Note that when handling relative links, the input document must have an appropriate `base URI` set when
     * parsing, so that the link's protocol can be confirmed. Regardless of the setting of the `preserve relative
     * links` option, the link must be resolvable against the base URI to an allowed protocol; otherwise the attribute
     * will be removed.
     *
     *
     * @param preserve `true` to allow relative links, `false` (default) to deny
     * @return this Safelist, for chaining.
     * @see .addProtocols
     */
    public fun preserveRelativeLinks(preserve: Boolean): Safelist {
        preserveRelativeLinks = preserve
        return this
    }

    /**
     * Add allowed URL protocols for an element's URL attribute. This restricts the possible values of the attribute to
     * URLs with the defined protocol.
     *
     *
     * E.g.: `addProtocols("a", "href", "ftp", "http", "https")`
     *
     *
     *
     * To allow a link to an in-page URL anchor (i.e. `<a href="#anchor">`, add a `#`:<br></br>
     * E.g.: `addProtocols("a", "href", "#")`
     *
     *
     * @param tag       Tag the URL protocol is for
     * @param attribute       Attribute name
     * @param protocols List of valid protocols
     * @return this, for chaining
     */
    public fun addProtocols(
        tag: String,
        attribute: String,
        vararg protocols: String,
    ): Safelist {
        Validate.notEmpty(tag)
        Validate.notEmpty(attribute)
        val tagName = TagName.valueOf(tag)
        val attrKey = AttributeKey.valueOf(attribute)
        val attrMap: MutableMap<AttributeKey, MutableSet<Protocol>>
        val protSet: MutableSet<Protocol>
        if (this.protocols.containsKey(tagName)) {
            attrMap = this.protocols[tagName]!!
        } else {
            attrMap = mutableMapOf()
            this.protocols[tagName] = attrMap
        }
        if (attrMap.containsKey(attrKey)) {
            protSet = attrMap[attrKey]!!
        } else {
            protSet = HashSet<Protocol>()
            attrMap[attrKey] = protSet
        }
        for (protocol in protocols) {
            Validate.notEmpty(protocol)
            val prot = Protocol.valueOf(protocol)
            protSet.add(prot)
        }
        return this
    }

    /**
     * Remove allowed URL protocols for an element's URL attribute. If you remove all protocols for an attribute, that
     * attribute will allow any protocol.
     *
     *
     * E.g.: `removeProtocols("a", "href", "ftp")`
     *
     *
     * @param tag Tag the URL protocol is for
     * @param attribute Attribute name
     * @param removeProtocols List of invalid protocols
     * @return this, for chaining
     */
    public fun removeProtocols(
        tag: String,
        attribute: String,
        vararg removeProtocols: String,
    ): Safelist {
        Validate.notEmpty(tag)
        Validate.notEmpty(attribute)
        val tagName = TagName.valueOf(tag)
        val attr = AttributeKey.valueOf(attribute)

        // make sure that what we're removing actually exists; otherwise can open the tag to any data and that can
        // be surprising
        Validate.isTrue(protocols.containsKey(tagName), "Cannot remove a protocol that is not set.")
        val tagProtocols: MutableMap<AttributeKey, MutableSet<Protocol>> = protocols[tagName]!!
        Validate.isTrue(tagProtocols.containsKey(attr), "Cannot remove a protocol that is not set.")
        val attrProtocols: MutableSet<Protocol> = tagProtocols[attr]!!
        for (protocol in removeProtocols) {
            Validate.notEmpty(protocol)
            attrProtocols.remove(Protocol.valueOf(protocol))
        }
        if (attrProtocols.isEmpty()) { // Remove protocol set if empty
            tagProtocols.remove(attr)
            if (tagProtocols.isEmpty()) {
                // Remove entry for tag if empty
                protocols.remove(tagName)
            }
        }
        return this
    }

    /**
     * Test if the supplied tag is allowed by this safelist.
     * @param tag test tag
     * @return true if allowed
     */
    public open fun isSafeTag(tag: String): Boolean {
        return tagNames.contains(TagName.valueOf(tag))
    }

    /**
     * Test if the supplied attribute is allowed by this safelist for this tag.
     * @param tagName tag to consider allowing the attribute in
     * @param el element under test, to confirm protocol
     * @param attr attribute under test
     * @return true if allowed
     */
    public open fun isSafeAttribute(
        tagName: String,
        el: Element,
        attr: Attribute,
    ): Boolean {
        val tag = TagName.valueOf(tagName)
        val key = AttributeKey.valueOf(attr.key)
        val okSet: Set<AttributeKey>? = attributes[tag]
        if (okSet != null && okSet.contains(key)) {
            return if (protocols.containsKey(tag)) {
                val attrProts: Map<AttributeKey, MutableSet<Protocol>> = protocols[tag]!!
                // ok if not defined protocol; otherwise test
                !attrProts.containsKey(key) || testValidProtocol(el, attr, attrProts[key]!!)
            } else { // attribute found, no protocols defined, so OK
                true
            }
        }
        // might be an enforced attribute?
        val enforcedSet: Map<AttributeKey, AttributeValue>? = enforcedAttributes[tag]
        if (enforcedSet != null) {
            val expect: Attributes = getEnforcedAttributes(tagName)
            val attrKey: String = attr.key
            if (expect.hasKeyIgnoreCase(attrKey)) {
                return expect.getIgnoreCase(attrKey).equals(attr.value)
            }
        }
        // no attributes defined for tag, try :all tag
        return tagName != All && isSafeAttribute(All, el, attr)
    }

    private fun testValidProtocol(
        el: Element,
        attr: Attribute,
        protocols: Set<Protocol>,
    ): Boolean {
        // try to resolve relative urls to abs, and optionally update the attribute so output html has abs.
        // rels without a baseuri get removed
        var value: String = el.absUrl(attr.key)
        if (value.isEmpty()) {
            value =
                attr.value // if it could not be made abs, run as-is to allow custom unknown protocols
        }
        if (!preserveRelativeLinks) attr.setValue(value)
        for (protocol in protocols) {
            var prot = protocol.toString()
            if (prot == "#") { // allows anchor links
                return if (isValidAnchor(value)) {
                    true
                } else {
                    continue
                }
            }
            prot += ":"
            if (lowerCase(value).startsWith(prot)) {
                return true
            }
        }
        return false
    }

    private fun isValidAnchor(value: String): Boolean {
        return value.startsWith("#") && !value.matches(".*\\s.*".toRegex())
    }

    /**
     * Gets the Attributes that should be enforced for a given tag
     * @param tagName the tag
     * @return the attributes that will be enforced; empty if none are set for the given tag
     */
    public fun getEnforcedAttributes(tagName: String): Attributes {
        val attrs = Attributes()
        val tag = TagName.valueOf(tagName)
        if (enforcedAttributes.containsKey(tag)) {
            val keyVals: Map<AttributeKey, AttributeValue> = enforcedAttributes[tag]!!
            for ((key, value) in keyVals) {
                attrs.put(key.toString(), value.toString())
            }
        }
        return attrs
    }

    // named types for config. All just hold strings, but here for my sanity.
    internal class TagName(value: String) : TypedValue(value) {
        companion object {
            fun valueOf(value: String): TagName {
                return TagName(value)
            }
        }
    }

    internal class AttributeKey(value: String) : TypedValue(value) {
        companion object {
            fun valueOf(value: String): AttributeKey {
                return AttributeKey(value)
            }
        }
    }

    internal class AttributeValue(value: String) : TypedValue(value) {
        companion object {
            fun valueOf(value: String): AttributeValue {
                return AttributeValue(value)
            }
        }
    }

    internal class Protocol(value: String) : TypedValue(value) {
        companion object {
            fun valueOf(value: String): Protocol {
                return Protocol(value)
            }
        }
    }

    internal abstract class TypedValue(value: String) {
        private val value: String

        init {
            this.value = value
        }

        override fun hashCode(): Int {
            val prime = 31
            var result = 1
            result = prime * result + value.hashCode()
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null) return false
            if (this::class != other::class) return false
            val obj = other as TypedValue
            return value == obj.value
        }

        override fun toString(): String {
            return value
        }
    }

    public companion object {
        private const val All = ":all"

        /**
         * This safelist allows only text nodes: any HTML Element or any Node other than a TextNode will be removed.
         *
         *
         * Note that the output of [com.fleeksoft.ksoup.Ksoup.clean] is still **HTML** even when using
         * this Safelist, and so any HTML entities in the output will be appropriately escaped. If you want plain text, not
         * HTML, you should use a text method such as [Element.text] instead, after cleaning the document.
         *
         *
         * Example:
         * <pre>`String sourceBodyHtml = "<p>5 is &lt; 6.</p>";
         * String html = Ksoup.clean(sourceBodyHtml, Safelist.none());
         *
         * Cleaner cleaner = new Cleaner(Safelist.none());
         * String text = cleaner.clean(Ksoup.parse(sourceBodyHtml)).text();
         *
         * // html is: 5 is &lt; 6.
         * // text is: 5 is < 6.
         `</pre> *
         *
         * @return safelist
         */
        public fun none(): Safelist {
            return Safelist()
        }

        /**
         * This safelist allows only simple text formatting: `b, em, i, strong, u`. All other HTML (tags and
         * attributes) will be removed.
         *
         * @return safelist
         */
        public fun simpleText(): Safelist {
            return Safelist()
                .addTags("b", "em", "i", "strong", "u")
        }

        /**
         *
         *
         * This safelist allows a fuller range of text nodes: `a, b, blockquote, br, cite, code, dd, dl, dt, em, i, li,
         * ol, p, pre, q, small, span, strike, strong, sub, sup, u, ul`, and appropriate attributes.
         *
         *
         *
         * Links (`a` elements) can point to `http, https, ftp, mailto`, and have an enforced
         * `rel=nofollow` attribute.
         *
         *
         *
         * Does not allow images.
         *
         *
         * @return safelist
         */
        public fun basic(): Safelist {
            return Safelist()
                .addTags(
                    "a", "b", "blockquote", "br", "cite", "code", "dd", "dl", "dt", "em",
                    "i", "li", "ol", "p", "pre", "q", "small", "span", "strike", "strong", "sub",
                    "sup", "u", "ul",
                )
                .addAttributes("a", "href")
                .addAttributes("blockquote", "cite")
                .addAttributes("q", "cite")
                .addProtocols("a", "href", "ftp", "http", "https", "mailto")
                .addProtocols("blockquote", "cite", "http", "https")
                .addProtocols("cite", "cite", "http", "https")
                .addEnforcedAttribute("a", "rel", "nofollow")
        }

        /**
         * This safelist allows the same text tags as [.basic], and also allows `img` tags, with appropriate
         * attributes, with `src` pointing to `http` or `https`.
         *
         * @return safelist
         */
        public fun basicWithImages(): Safelist {
            return basic()
                .addTags("img")
                .addAttributes("img", "align", "alt", "height", "src", "title", "width")
                .addProtocols("img", "src", "http", "https")
        }

        /**
         * This safelist allows a full range of text and structural body HTML: `a, b, blockquote, br, caption, cite,
         * code, col, colgroup, dd, div, dl, dt, em, h1, h2, h3, h4, h5, h6, i, img, li, ol, p, pre, q, small, span, strike, strong, sub,
         * sup, table, tbody, td, tfoot, th, thead, tr, u, ul`
         *
         *
         * Links do not have an enforced `rel=nofollow` attribute, but you can add that if desired.
         *
         *
         * @return safelist
         */
        public fun relaxed(): Safelist {
            return Safelist()
                .addTags(
                    "a", "b", "blockquote", "br", "caption", "cite", "code", "col",
                    "colgroup", "dd", "div", "dl", "dt", "em", "h1", "h2", "h3", "h4", "h5", "h6",
                    "i", "img", "li", "ol", "p", "pre", "q", "small", "span", "strike", "strong",
                    "sub", "sup", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "u",
                    "ul",
                )
                .addAttributes("a", "href", "title")
                .addAttributes("blockquote", "cite")
                .addAttributes("col", "span", "width")
                .addAttributes("colgroup", "span", "width")
                .addAttributes("img", "align", "alt", "height", "src", "title", "width")
                .addAttributes("ol", "start", "type")
                .addAttributes("q", "cite")
                .addAttributes("table", "summary", "width")
                .addAttributes("td", "abbr", "axis", "colspan", "rowspan", "width")
                .addAttributes(
                    "th",
                    "abbr",
                    "axis",
                    "colspan",
                    "rowspan",
                    "scope",
                    "width",
                )
                .addAttributes("ul", "type")
                .addProtocols("a", "href", "ftp", "http", "https", "mailto")
                .addProtocols("blockquote", "cite", "http", "https")
                .addProtocols("cite", "cite", "http", "https")
                .addProtocols("img", "src", "http", "https")
                .addProtocols("q", "cite", "http", "https")
        }
    }
}
