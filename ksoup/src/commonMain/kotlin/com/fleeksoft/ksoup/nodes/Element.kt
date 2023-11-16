package com.fleeksoft.ksoup.nodes

import okio.IOException
import com.fleeksoft.ksoup.helper.ChangeNotifyingArrayList
import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.Normalizer.normalize
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.nodes.TextNode.Companion.lastCharIsWhitespace
import com.fleeksoft.ksoup.parser.ParseSettings
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.parser.Tag
import com.fleeksoft.ksoup.parser.TokenQueue.Companion.escapeCssIdentifier
import com.fleeksoft.ksoup.select.Collector
import com.fleeksoft.ksoup.select.Elements
import com.fleeksoft.ksoup.select.Evaluator
import com.fleeksoft.ksoup.select.NodeFilter
import com.fleeksoft.ksoup.select.NodeTraversor
import com.fleeksoft.ksoup.select.NodeVisitor
import com.fleeksoft.ksoup.select.QueryParser
import com.fleeksoft.ksoup.select.Selector
import com.fleeksoft.ksoup.ported.AtomicBoolean
import com.fleeksoft.ksoup.ported.Collections
import com.fleeksoft.ksoup.ported.Consumer
import com.fleeksoft.ksoup.ported.PatternSyntaxException
import kotlin.jvm.JvmOverloads

/**
 * An HTML Element consists of a tag name, attributes, and child nodes (including text nodes and other elements).
 *
 *
 * From an Element, you can extract data, traverse the node graph, and manipulate the HTML.
 */

public open class Element : Node {
    private var tag: Tag
    private var _baseUri: String? = null // just for clone

    // points to child elements shadowed from node children
    private var shadowChildrenRef: List<Element>? = null
    internal var childNodes: MutableList<Node> = EmptyNodes

    // field is nullable but all methods for attributes are non-null
    internal var attributes: Attributes? = null

    /**
     * Create a new, standalone element, in the specified namespace.
     * @param tag tag name
     * @param namespace namespace for this element
     */
    public constructor(tag: String, namespace: String) : this(
        Tag.valueOf(
            tag,
            namespace,
            ParseSettings.preserveCase,
        ),
        null,
    )

    /**
     * Create a new, standalone element, in the HTML namespace.
     * @param tag tag name
     * @see .Element
     */
    public constructor(tag: String) : this(
        Tag.valueOf(
            tag,
            Parser.NamespaceHtml,
            ParseSettings.preserveCase,
        ),
        "",
        null,
    )

    /**
     * Create a new, standalone Element. (Standalone in that it has no parent.)
     *
     * @param tag tag of this element
     * @param baseUri the base URI (optional, may be null to inherit from parent, or "" to clear parent's)
     * @param attributes initial attributes (optional, may be null)
     * @see #appendChild(Node)
     * @see #appendElement(String)
     */
    public constructor(tag: Tag, baseUri: String?, attributes: Attributes?) {
        childNodes = EmptyNodes.toMutableList()
        this.attributes = attributes
        this.tag = tag
        _baseUri = baseUri
        if (baseUri != null) this.setBaseUri(baseUri)
    }

    /**
     * Create a new Element from a Tag and a base URI.
     *
     * @param tag element tag
     * @param baseUri the base URI of this element. Optional, and will inherit from its parent, if any.
     * @see Tag.valueOf
     */
    public constructor(tag: Tag, baseUri: String?) : this(tag, baseUri, null)

    /**
     * Internal test to check if a nodelist object has been created.
     */
    public fun hasChildNodes(): Boolean {
        return childNodes != EmptyNodes
    }

    public override fun ensureChildNodes(): MutableList<Node> {
        if (childNodes == EmptyNodes) {
            childNodes = NodeList(owner = this, initialCapacity = 4) as MutableList<Node>
        }
        return childNodes
    }

    public override fun hasAttributes(): Boolean {
        return attributes != null
    }

    override fun attributes(): Attributes {
        if (attributes == null) {
            // not using hasAttributes, as doesn't clear warning
            attributes = Attributes()
        }
        return attributes!!
    }

    override fun baseUri(): String {
        return searchUpForAttribute(this, BaseUriKey)
    }

    public override fun doSetBaseUri(baseUri: String?) {
        attributes().put(BaseUriKey, baseUri)
    }

    override fun childNodeSize(): Int {
        return childNodes.size
    }

    override fun nodeName(): String {
        return tag.name
    }

    /**
     * Get the name of the tag for this element. E.g. `div`. If you are using [ case preserving parsing][ParseSettings.preserveCase], this will return the source's original case.
     *
     * @return the tag name
     */
    public fun tagName(): String {
        return tag.name
    }

    /**
     * Get the normalized name of this Element's tag. This will always be the lower-cased version of the tag, regardless
     * of the tag case preserving setting of the parser. For e.g., `<DIV>` and `<div>` both have a
     * normal name of `div`.
     * @return normal name
     */
    override fun normalName(): String {
        return tag.normalName()
    }


    /**
     * Change (rename) the tag of this element. For example, convert a `<span>` to a `<div>` with
     * `el.tagName("div");`.
     *
     * @param tagName new tag name for this element
     * @param namespace the new namespace for this element
     * @return this element, for chaining
     * @see Elements.tagName
     */
    /**
     * Change (rename) the tag of this element. For example, convert a `<span>` to a `<div>` with
     * `el.tagName("div");`.
     *
     * @param tagName new tag name for this element
     * @return this element, for chaining
     * @see Elements.tagName
     */
    @JvmOverloads
    public fun tagName(tagName: String, namespace: String = tag.namespace()): Element {
        Validate.notEmptyParam(tagName, "tagName")
        Validate.notEmptyParam(namespace, "namespace")
        tag = Tag.valueOf(
            tagName,
            namespace,
            NodeUtils.parser(this).settings(),
        ) // maintains the case option of the original parse
        return this
    }

    /**
     * Get the Tag for this element.
     *
     * @return the tag object
     */
    public fun tag(): Tag {
        return tag
    }

    public fun isBlock(): Boolean = tag.isBlock

    /**
     * Get the `id` attribute of this element.
     *
     * @return The id attribute, if present, or an empty string if not.
     */
    public fun id(): String {
        return if (attributes != null) attributes!!.getIgnoreCase("id") else ""
    }

    /**
     * Set the `id` attribute of this element.
     * @param id the ID value to use
     * @return this Element, for chaining
     */
    public fun id(id: String): Element {
        attr("id", id)
        return this
    }

    /**
     * Set an attribute value on this element. If this element already has an attribute with the
     * key, its value is updated; otherwise, a new attribute is added.
     *
     * @return this element
     */
    override fun attr(attributeKey: String, attributeValue: String?): Element {
        super.attr(attributeKey, attributeValue)
        return this
    }

    /**
     * Set a boolean attribute value on this element. Setting to `true` sets the attribute value to "" and
     * marks the attribute as boolean so no value is written out. Setting to `false` removes the attribute
     * with the same key if it exists.
     *
     * @param attributeKey the attribute key
     * @param attributeValue the attribute value
     *
     * @return this element
     */
    public fun attr(attributeKey: String, attributeValue: Boolean): Element {
        attributes().put(attributeKey, attributeValue)
        return this
    }

    /**
     * Get this element's HTML5 custom data attributes. Each attribute in the element that has a key
     * starting with "data-" is included the dataset.
     *
     *
     * E.g., the element `<div data-package="com.fleeksoft.ksoup" data-language="Java" class="group">...` has the dataset
     * `package=com.fleeksoft.ksoup, language=java`.
     *
     *
     * This map is a filtered view of the element's attribute map. Changes to one map (add, remove, update) are reflected
     * in the other map.
     *
     *
     * You can find elements that have data attributes using the `[^data-]` attribute key prefix selector.
     * @return a map of `key=value` custom data attributes.
     */
    public fun dataset(): Attributes.Dataset {
        return attributes().dataset()
    }

    override fun parent(): Element? {
        return _parentNode as? Element
    }

    /**
     * Get this element's parent and ancestors, up to the document root.
     * @return this element's stack of parents, starting with the closest first.
     */
    public fun parents(): Elements {
        val parents = Elements()
        var parent = parent()
        while (parent != null && !parent.isNode("#root")) {
            parents.add(parent)
            parent = parent.parent()
        }
        return parents
    }

    /**
     * Get a child element of this element, by its 0-based index number.
     *
     *
     * Note that an element can have both mixed Nodes and Elements as children. This method inspects
     * a filtered list of children that are elements, and the index is based on that filtered list.
     *
     *
     * @param index the index number of the element to retrieve
     * @return the child element, if it exists, otherwise throws an `IndexOutOfBoundsException`
     * @see .childNode
     */
    public fun child(index: Int): Element {
        return childElementsList()[index]
    }

    /**
     * Get the number of child nodes of this element that are elements.
     *
     *
     * This method works on the same filtered list like [.child]. Use [.childNodes] and [ ][.childNodeSize] to get the unfiltered Nodes (e.g. includes TextNodes etc.)
     *
     *
     * @return the number of child nodes that are elements
     * @see .children
     * @see .child
     */
    public fun childrenSize(): Int {
        return childElementsList().size
    }

    /**
     * Get this element's child elements.
     *
     *
     * This is effectively a filter on [.childNodes] to get Element nodes.
     *
     * @return child elements. If this element has no children, returns an empty list.
     * @see .childNodes
     */
    public fun children(): Elements {
        return Elements(childElementsList())
    }

    /**
     * Maintains a shadow copy of this element's child elements. If the nodelist is changed, this cache is invalidated.
     * TODO - think about pulling this out as a helper as there are other shadow lists (like in Attributes) kept around.
     * @return a list of child elements
     */
    public fun childElementsList(): List<Element> {
        if (childNodeSize() == 0) return EmptyChildren // short circuit creating empty
        var children: MutableList<Element>? = null
        if (shadowChildrenRef != null) {
            children = shadowChildrenRef!!.toMutableList()
        }
        if (shadowChildrenRef == null || children == null) {
            val size = childNodes.size
            children = ArrayList(size)
            for (i in 0 until size) {
                val node: Node = childNodes[i]
                if (node is Element) children.add(node)
            }
            shadowChildrenRef = children
        }
        return children
    }

    /**
     * Clears the cached shadow child elements.
     */
    override fun nodelistChanged() {
        super.nodelistChanged()
        shadowChildrenRef = null
    }

    /**
     * Get this element's child text nodes. The list is unmodifiable but the text nodes may be manipulated.
     *
     *
     * This is effectively a filter on [.childNodes] to get Text nodes.
     * @return child text nodes. If this element has no text nodes, returns an
     * empty list.
     *
     * For example, with the input HTML: `<p>One <span>Two</span> Three <br> Four</p>` with the `p` element selected:
     *
     *  * `p.text()` = `"One Two Three Four"`
     *  * `p.ownText()` = `"One Three Four"`
     *  * `p.children()` = `Elements[<span>, <br>]`
     *  * `p.childNodes()` = `List<Node>["One ", <span>, " Three ", <br>, " Four"]`
     *  * `p.textNodes()` = `List<TextNode>["One ", " Three ", " Four"]`
     *
     */
    public fun textNodes(): List<TextNode> {
        val textNodes: MutableList<TextNode> = ArrayList()
        for (node in childNodes) {
            if (node is TextNode) textNodes.add(node)
        }
        return Collections.unmodifiableList(textNodes)
    }

    /**
     * Get this element's child data nodes. The list is unmodifiable but the data nodes may be manipulated.
     *
     *
     * This is effectively a filter on [.childNodes] to get Data nodes.
     *
     * @return child data nodes. If this element has no data nodes, returns an
     * empty list.
     * @see .data
     */
    public fun dataNodes(): List<DataNode> {
        val dataNodes: MutableList<DataNode> = ArrayList()
        for (node in childNodes) {
            if (node is DataNode) dataNodes.add(node)
        }
        return Collections.unmodifiableList(dataNodes)
    }

    /**
     * Find elements that match the [Selector] CSS query, with this element as the starting context. Matched elements
     * may include this element, or any of its children.
     *
     * This method is generally more powerful to use than the DOM-type `getElementBy*` methods, because
     * multiple filters can be combined, e.g.:
     *
     *  * `el.select("a[href]")` - finds links (`a` tags with `href` attributes)
     *  * `el.select("a[href*=example.com]")` - finds links pointing to example.com (loosely)
     *
     *
     * See the query syntax documentation in [com.fleeksoft.ksoup.select.Selector].
     *
     * Also known as `querySelectorAll()` in the Web DOM.
     *
     * @param cssQuery a [Selector] CSS-like query
     * @return an [Elements] list containing elements that match the query (empty if none match)
     * @see Selector selector query syntax
     *
     * @see QueryParser.parse
     * @throws Selector.SelectorParseException (unchecked) on an invalid CSS query.
     */
    public fun select(cssQuery: String): Elements {
        return Selector.select(cssQuery, this)
    }

    /**
     * Find elements that match the supplied Evaluator. This has the same functionality as [.select], but
     * may be useful if you are running the same query many times (on many documents) and want to save the overhead of
     * repeatedly parsing the CSS query.
     * @param evaluator an element evaluator
     * @return an [Elements] list containing elements that match the query (empty if none match)
     */
    internal fun select(evaluator: Evaluator): Elements {
        return Selector.select(evaluator, this)
    }

    /**
     * Find the first Element that matches the [Selector] CSS query, with this element as the starting context.
     *
     * This is effectively the same as calling `element.select(query).first()`, but is more efficient as query
     * execution stops on the first hit.
     *
     * Also known as `querySelector()` in the Web DOM.
     * @param cssQuery cssQuery a [Selector] CSS-like query
     * @return the first matching element, or **`null`** if there is no match.
     * @see .expectFirst
     */
    public fun selectFirst(cssQuery: String): Element? {
        return Selector.selectFirst(cssQuery, this)
    }

    /**
     * Finds the first Element that matches the supplied Evaluator, with this element as the starting context, or
     * `null` if none match.
     *
     * @param evaluator an element evaluator
     * @return the first matching element (walking down the tree, starting from this element), or `null` if none
     * match.
     */
    internal fun selectFirst(evaluator: Evaluator): Element? {
        return Collector.findFirst(evaluator, this)
    }

    /**
     * Just like [.selectFirst], but if there is no match, throws an [IllegalArgumentException]. This
     * is useful if you want to simply abort processing on a failed match.
     * @param cssQuery a [Selector] CSS-like query
     * @return the first matching element
     * @throws IllegalArgumentException if no match is found
     * @since 1.15.2
     */
    public fun expectFirst(cssQuery: String): Element {
        return Validate.ensureNotNull(
            Selector.selectFirst(cssQuery, this),
            if (parent() != null) "No elements matched the query '$cssQuery' on element '${this.tagName()}'." else "No elements matched the query '$cssQuery' in the document.",
        ) as Element
    }

    /**
     * Checks if this element matches the given [Selector] CSS query. Also knows as `matches()` in the Web
     * DOM.
     *
     * @param cssQuery a [Selector] CSS query
     * @return if this element matches the query
     */
    public fun `is`(cssQuery: String): Boolean {
        return `is`(QueryParser.parse(cssQuery))
    }

    /**
     * Check if this element matches the given evaluator.
     * @param evaluator an element evaluator
     * @return if this element matches
     */
    internal fun `is`(evaluator: Evaluator?): Boolean {
        return evaluator!!.matches(root(), this)
    }

    /**
     * Find the closest element up the tree of parents that matches the specified CSS query. Will return itself, an
     * ancestor, or `null` if there is no such matching element.
     * @param cssQuery a [Selector] CSS query
     * @return the closest ancestor element (possibly itself) that matches the provided evaluator. `null` if not
     * found.
     */
    public fun closest(cssQuery: String): Element? {
        return closest(QueryParser.parse(cssQuery))
    }

    /**
     * Find the closest element up the tree of parents that matches the specified evaluator. Will return itself, an
     * ancestor, or `null` if there is no such matching element.
     * @param evaluator a query evaluator
     * @return the closest ancestor element (possibly itself) that matches the provided evaluator. `null` if not
     * found.
     */
//    @Nullable
    internal fun closest(evaluator: Evaluator): Element? {
        var el: Element? = this
        val root = root()
        do {
            if (evaluator.matches(root, el!!)) return el
            el = el.parent()
        } while (el != null)
        return null
    }

    /**
     * Insert a node to the end of this Element's children. The incoming node will be re-parented.
     *
     * @param child node to add.
     * @return this Element, for chaining
     * @see .prependChild
     * @see .insertChildren
     */
    public fun appendChild(child: Node): Element {
        // was - Node#addChildren(child). short-circuits an array create and a loop.
        reparentChild(child)
        ensureChildNodes()
        childNodes.add(child)
        child.siblingIndex = childNodes.size - 1
        return this
    }

    /**
     * Insert the given nodes to the end of this Element's children.
     *
     * @param children nodes to add
     * @return this Element, for chaining
     * @see .insertChildren
     */
    public fun appendChildren(children: Collection<Node>): Element {
        insertChildren(-1, children)
        return this
    }

    /**
     * Add this element to the supplied parent element, as its next child.
     *
     * @param parent element to which this element will be appended
     * @return this element, so that you can continue modifying the element
     */
    public fun appendTo(parent: Element): Element {
        parent.appendChild(this)
        return this
    }

    /**
     * Add a node to the start of this element's children.
     *
     * @param child node to add.
     * @return this element, so that you can add more child nodes or elements.
     */
    public fun prependChild(child: Node): Element {
        addChildren(0, child)
        return this
    }

    /**
     * Insert the given nodes to the start of this Element's children.
     *
     * @param children nodes to add
     * @return this Element, for chaining
     * @see .insertChildren
     */
    public fun prependChildren(children: Collection<Node>): Element {
        insertChildren(0, children)
        return this
    }

    /**
     * Inserts the given child nodes into this element at the specified index. Current nodes will be shifted to the
     * right. The inserted nodes will be moved from their current parent. To prevent moving, copy the nodes first.
     *
     * @param index 0-based index to insert children at. Specify `0` to insert at the start, `-1` at the
     * end
     * @param children child nodes to insert
     * @return this element, for chaining.
     */
    public fun insertChildren(index: Int, children: Collection<Node>): Element {
        var index = index
        val currentSize = childNodeSize()
        if (index < 0) index += currentSize + 1 // roll around
        Validate.isTrue(index in 0..currentSize, "Insert position out of bounds.")
        val nodeArray: Array<Node> = children.toTypedArray()
        addChildren(index, *nodeArray)
        return this
    }

    /**
     * Inserts the given child nodes into this element at the specified index. Current nodes will be shifted to the
     * right. The inserted nodes will be moved from their current parent. To prevent moving, copy the nodes first.
     *
     * @param index 0-based index to insert children at. Specify `0` to insert at the start, `-1` at the
     * end
     * @param children child nodes to insert
     * @return this element, for chaining.
     */
    public fun insertChildren(index: Int, vararg children: Node): Element {
        var index = index
        val currentSize = childNodeSize()
        if (index < 0) index += currentSize + 1 // roll around
        Validate.isTrue(index in 0..currentSize, "Insert position out of bounds.")
        addChildren(index, *children)
        return this
    }

    /**
     * Create a new element by tag name, and add it as the last child.
     *
     * @param tagName the name of the tag (e.g. `div`).
     * @return the new element, to allow you to add content to it, e.g.:
     * `parent.appendElement("h1").attr("id", "header").text("Welcome");`
     */
    @JvmOverloads
    public fun appendElement(tagName: String, namespace: String = tag.namespace()): Element {
        val child = Element(
            Tag.valueOf(
                tagName,
                namespace,
                NodeUtils.parser(this)
                    .settings(),
            ),
            baseUri(),
        )
        appendChild(child)
        return child
    }

    /**
     * Create a new element by tag name, and add it as the first child.
     *
     * @param tagName the name of the tag (e.g. `div`).
     * @return the new element, to allow you to add content to it, e.g.:
     * `parent.prependElement("h1").attr("id", "header").text("Welcome");`
     */
    @JvmOverloads
    public fun prependElement(tagName: String, namespace: String = tag.namespace()): Element {
        val child = Element(
            Tag.valueOf(
                tagName,
                namespace,
                NodeUtils.parser(this)
                    .settings(),
            ),
            baseUri(),
        )
        prependChild(child)
        return child
    }

    /**
     * Create and append a new TextNode to this element.
     *
     * @param text the (un-encoded) text to add
     * @return this element
     */
    public fun appendText(text: String): Element {
        val node = TextNode(text)
        appendChild(node)
        return this
    }

    /**
     * Create and prepend a new TextNode to this element.
     *
     * @param text the decoded text to add
     * @return this element
     */
    public fun prependText(text: String): Element {
        val node = TextNode(text)
        prependChild(node)
        return this
    }

    /**
     * Add inner HTML to this element. The supplied HTML will be parsed, and each node appended to the end of the children.
     * @param html HTML to add inside this element, after the existing HTML
     * @return this element
     * @see .html
     */
    public fun append(html: String): Element {
        val nodes: List<Node> = NodeUtils.parser(this).parseFragmentInput(html, this, baseUri())
        addChildren(*nodes.toTypedArray())
        return this
    }

    /**
     * Add inner HTML into this element. The supplied HTML will be parsed, and each node prepended to the start of the element's children.
     * @param html HTML to add inside this element, before the existing HTML
     * @return this element
     * @see .html
     */
    public fun prepend(html: String): Element {
        val nodes: List<Node> = NodeUtils.parser(this).parseFragmentInput(html, this, baseUri())
        addChildren(0, *nodes.toTypedArray())
        return this
    }

    /**
     * Insert the specified HTML into the DOM before this element (as a preceding sibling).
     *
     * @param html HTML to add before this element
     * @return this element, for chaining
     * @see .after
     */
    override fun before(html: String): Element {
        return super.before(html) as Element
    }

    /**
     * Insert the specified node into the DOM before this node (as a preceding sibling).
     * @param node to add before this element
     * @return this Element, for chaining
     * @see .after
     */
    override fun before(node: Node): Element {
        return super.before(node) as Element
    }

    /**
     * Insert the specified HTML into the DOM after this element (as a following sibling).
     *
     * @param html HTML to add after this element
     * @return this element, for chaining
     * @see .before
     */
    override fun after(html: String): Element {
        return super.after(html) as Element
    }

    /**
     * Insert the specified node into the DOM after this node (as a following sibling).
     * @param node to add after this element
     * @return this element, for chaining
     * @see .before
     */
    override fun after(node: Node): Element {
        return super.after(node) as Element
    }

    /**
     * Remove all the element's child nodes. Any attributes are left as-is. Each child node has its parent set to
     * `null`.
     * @return this element
     */
    override fun empty(): Element {
        // Detach each of the children -> parent links:
        for (child in childNodes) {
            child._parentNode = null
        }
        childNodes.clear()
        return this
    }

    /**
     * Wrap the supplied HTML around this element.
     *
     * @param html HTML to wrap around this element, e.g. `<div class="head"></div>`. Can be arbitrarily deep.
     * @return this element, for chaining.
     */
    override fun wrap(html: String): Element {
        return super.wrap(html) as Element
    }

    /**
     * Get a CSS selector that will uniquely select this element.
     *
     *
     * If the element has an ID, returns #id;
     * otherwise returns the parent (if any) CSS selector, followed by &#39;&gt;&#39;,
     * followed by a unique selector for the element (tag.class.class:nth-child(n)).
     *
     *
     * @return the CSS Path that can be used to retrieve the element in a selector.
     */
    public fun cssSelector(): String {
        if (id().isNotEmpty()) {
            // prefer to return the ID - but check that it's actually unique first!
            val idSel = "#" + escapeCssIdentifier(id())
            val doc: Document? = ownerDocument()
            if (doc != null) {
                val els: Elements = doc.select(idSel)
                if (els.size === 1 && els[0] === this) {
                    // otherwise, continue to the nth-child impl
                    return idSel
                }
            } else {
                return idSel // no ownerdoc, return the ID selector
            }
        }
        val selector: StringBuilder = StringUtil.borrowBuilder()
        var el: Element? = this
        while (el != null && el !is Document) {
            selector.insert(0, el.cssSelectorComponent())
            el = el.parent()
        }
        return StringUtil.releaseBuilder(selector)
    }

    private fun cssSelectorComponent(): String {
        // Escape tagname, and translate HTML namespace ns:tag to CSS namespace syntax ns|tag
        val tagName: String = escapeCssIdentifier(tagName()).replace("\\:", "|")
        val selector: StringBuilder = StringUtil.borrowBuilder().append(tagName)
        // String classes = StringUtil.join(classNames().stream().map(TokenQueue::escapeCssIdentifier).iterator(), ".");
        // todo - replace with ^^ in 1.16.1 when we enable Android support for stream etc
        val escapedClasses: StringUtil.StringJoiner = StringUtil.StringJoiner(".")
        for (name in classNames()) escapedClasses.add(escapeCssIdentifier(name))
        val classes: String = escapedClasses.complete()
        if (classes.isNotEmpty()) selector.append('.').append(classes)
        val parent: Element? = parent()
        if (parent == null || parent is Document) {
            // don't add Document to selector, as will always have a html node
            return StringUtil.releaseBuilder(selector)
        }
        selector.insert(0, " > ")
        if (parent.select(selector.toString()).size > 1) {
            selector.append(":nth-child(${elementSiblingIndex() + 1})")
        }
        return StringUtil.releaseBuilder(selector)
    }

    /**
     * Get sibling elements. If the element has no sibling elements, returns an empty list. An element is not a sibling
     * of itself, so will not be included in the returned list.
     * @return sibling elements
     */
    public fun siblingElements(): Elements {
        if (_parentNode == null) return Elements()
        val elements = (_parentNode as Element).childElementsList()
        val siblings = Elements()
        for (el in elements) if (el !== this) siblings.add(el)
        return siblings
    }

    /**
     * Gets the next sibling element of this element. E.g., if a `div` contains two `p`s,
     * the `nextElementSibling` of the first `p` is the second `p`.
     *
     *
     * This is similar to [.nextSibling], but specifically finds only Elements
     *
     * @return the next element, or null if there is no next element
     * @see .previousElementSibling
     */
//    @Nullable
    public fun nextElementSibling(): Element? {
        var next: Node = this
        while (next.nextSibling()?.also { next = it } != null) {
            if (next is Element) return next as Element
        }
        return null
    }

    /**
     * Get each of the sibling elements that come after this element.
     *
     * @return each of the element siblings after this element, or an empty list if there are no next sibling elements
     */
    public fun nextElementSiblings(): Elements {
        return nextElementSiblings(true)
    }

    /**
     * Gets the previous element sibling of this element.
     * @return the previous element, or null if there is no previous element
     * @see .nextElementSibling
     */
//    @Nullable
    public fun previousElementSibling(): Element? {
        var prev: Node = this
        while (prev.previousSibling()?.also { prev = it } != null) {
            if (prev is Element) return prev as Element
        }
        return null
    }

    /**
     * Get each of the element siblings before this element.
     *
     * @return the previous element siblings, or an empty list if there are none.
     */
    public fun previousElementSiblings(): Elements {
        return nextElementSiblings(false)
    }

    private fun nextElementSiblings(next: Boolean): Elements {
        val els = Elements()
        if (_parentNode == null) return els
        els.add(this)
        return if (next) els.nextAll() else els.prevAll()
    }

    /**
     * Gets the first Element sibling of this element. That may be this element.
     * @return the first sibling that is an element (aka the parent's first element child)
     */
    public fun firstElementSibling(): Element? {
        val parent: Element? = parent()
        return if (parent != null) {
            parent.firstElementChild()
        } else {
            this // orphan is its own first sibling
        }
    }

    /**
     * Get the list index of this element in its element sibling list. I.e. if this is the first element
     * sibling, returns 0.
     * @return position in element sibling list
     */
    public fun elementSiblingIndex(): Int {
        val parent: Element? = parent()
        return if (parent == null) {
            0
        } else {
            indexInList(
                this,
                parent.childElementsList(),
            )
        }
    }

    /**
     * Gets the last element sibling of this element. That may be this element.
     * @return the last sibling that is an element (aka the parent's last element child)
     */
    public fun lastElementSibling(): Element? {
        val parent: Element? = parent()
        return if (parent != null) {
            parent.lastElementChild()
        } else {
            this
        }
    }

    /**
     * Gets the first child of this Element that is an Element, or `null` if there is none.
     * @return the first Element child node, or null.
     * @see .firstChild
     * @see .lastElementChild
     * @since 1.15.2
     */
//    @Nullable
    public fun firstElementChild(): Element? {
        var child: Node? = firstChild()
        while (child != null) {
            if (child is Element) return child
            child = child.nextSibling()
        }
        return null
    }

    /**
     * Gets the last child of this Element that is an Element, or @{code null} if there is none.
     * @return the last Element child node, or null.
     * @see .lastChild
     * @see .firstElementChild
     * @since 1.15.2
     */
//    @Nullable
    public fun lastElementChild(): Element? {
        var child: Node? = lastChild()
        while (child != null) {
            if (child is Element) return child
            child = child.previousSibling()
        }
        return null
    }
    // DOM type methods
    /**
     * Finds elements, including and recursively under this element, with the specified tag name.
     * @param tagName The tag name to search for (case insensitively).
     * @return a matching unmodifiable list of elements. Will be empty if this element and none of its children match.
     */
    public fun getElementsByTag(tagName: String?): Elements {
        var tagName = tagName
        Validate.notEmpty(tagName)
        tagName = normalize(tagName)
        return Collector.collect(Evaluator.Tag(tagName), this)
    }

    /**
     * Find an element by ID, including or under this element.
     *
     *
     * Note that this finds the first matching ID, starting with this element. If you search down from a different
     * starting point, it is possible to find a different element by ID. For unique element by ID within a Document,
     * use [Document.getElementById]
     * @param id The ID to search for.
     * @return The first matching element by ID, starting with this element, or null if none found.
     */
//    @Nullable
    public fun getElementById(id: String): Element? {
        Validate.notEmpty(id)
        val elements: Elements = Collector.collect(Evaluator.Id(id), this)
        return if (elements.size > 0) elements[0] else null
    }

    /**
     * Find elements that have this class, including or under this element. Case-insensitive.
     *
     *
     * Elements can have multiple classes (e.g. `<div class="header round first">`). This method
     * checks each class, so you can find the above with `el.getElementsByClass("header");`.
     *
     * @param className the name of the class to search for.
     * @return elements with the supplied class name, empty if none
     * @see .hasClass
     * @see .classNames
     */
    public fun getElementsByClass(className: String): Elements {
        Validate.notEmpty(className)
        return Collector.collect(Evaluator.Class(className), this)
    }

    /**
     * Find elements that have a named attribute set. Case-insensitive.
     *
     * @param key name of the attribute, e.g. `href`
     * @return elements that have this attribute, empty if none
     */
    public fun getElementsByAttribute(key: String): Elements {
        var key = key
        Validate.notEmpty(key)
        key = key.trim { it <= ' ' }
        return Collector.collect(Evaluator.Attribute(key), this)
    }

    /**
     * Find elements that have an attribute name starting with the supplied prefix. Use `data-` to find elements
     * that have HTML5 datasets.
     * @param keyPrefix name prefix of the attribute e.g. `data-`
     * @return elements that have attribute names that start with the prefix, empty if none.
     */
    public fun getElementsByAttributeStarting(keyPrefix: String): Elements {
        var keyPrefix = keyPrefix
        Validate.notEmpty(keyPrefix)
        keyPrefix = keyPrefix.trim { it <= ' ' }
        return Collector.collect(Evaluator.AttributeStarting(keyPrefix), this)
    }

    /**
     * Find elements that have an attribute with the specific value. Case-insensitive.
     *
     * @param key name of the attribute
     * @param value value of the attribute
     * @return elements that have this attribute with this value, empty if none
     */
    public fun getElementsByAttributeValue(key: String, value: String): Elements {
        return Collector.collect(Evaluator.AttributeWithValue(key, value), this)
    }

    /**
     * Find elements that either do not have this attribute, or have it with a different value. Case-insensitive.
     *
     * @param key name of the attribute
     * @param value value of the attribute
     * @return elements that do not have a matching attribute
     */
    public fun getElementsByAttributeValueNot(key: String, value: String): Elements {
        return Collector.collect(Evaluator.AttributeWithValueNot(key, value), this)
    }

    /**
     * Find elements that have attributes that start with the value prefix. Case-insensitive.
     *
     * @param key name of the attribute
     * @param valuePrefix start of attribute value
     * @return elements that have attributes that start with the value prefix
     */
    public fun getElementsByAttributeValueStarting(key: String, valuePrefix: String): Elements {
        return Collector.collect(Evaluator.AttributeWithValueStarting(key, valuePrefix), this)
    }

    /**
     * Find elements that have attributes that end with the value suffix. Case-insensitive.
     *
     * @param key name of the attribute
     * @param valueSuffix end of the attribute value
     * @return elements that have attributes that end with the value suffix
     */
    public fun getElementsByAttributeValueEnding(key: String, valueSuffix: String): Elements {
        return Collector.collect(Evaluator.AttributeWithValueEnding(key, valueSuffix), this)
    }

    /**
     * Find elements that have attributes whose value contains the match string. Case-insensitive.
     *
     * @param key name of the attribute
     * @param match substring of value to search for
     * @return elements that have attributes containing this text
     */
    public fun getElementsByAttributeValueContaining(key: String, match: String): Elements {
        return Collector.collect(Evaluator.AttributeWithValueContaining(key, match), this)
    }

    /**
     * Find elements that have an attribute whose value matches the supplied regular expression.
     * @param key name of the attribute
     * @param pattern compiled regular expression to match against attribute values
     * @return elements that have attributes matching this regular expression
     */
    public fun getElementsByAttributeValueMatching(
        key: String,
        regex: Regex,
    ): Elements {
        return Collector.collect(Evaluator.AttributeWithValueMatching(key, regex), this)
    }

    /**
     * Find elements that have attributes whose values match the supplied regular expression.
     * @param key name of the attribute
     * @param regex regular expression to match against attribute values. You can use [embedded flags](http://java.sun.com/docs/books/tutorial/essential/regex/pattern.html#embedded) (such as (?i) and (?m) to control regex options.
     * @return elements that have attributes matching this regular expression
     */
    public fun getElementsByAttributeValueMatching(key: String, regex: String): Elements {
        val pattern: Regex = try {
            Regex(regex)
        } catch (e: PatternSyntaxException) {
            throw IllegalArgumentException("Pattern syntax error: $regex", e)
        }
        return getElementsByAttributeValueMatching(key, pattern)
    }

    /**
     * Find elements whose sibling index is less than the supplied index.
     * @param index 0-based index
     * @return elements less than index
     */
    public fun getElementsByIndexLessThan(index: Int): Elements {
        return Collector.collect(Evaluator.IndexLessThan(index), this)
    }

    /**
     * Find elements whose sibling index is greater than the supplied index.
     * @param index 0-based index
     * @return elements greater than index
     */
    public fun getElementsByIndexGreaterThan(index: Int): Elements {
        return Collector.collect(Evaluator.IndexGreaterThan(index), this)
    }

    /**
     * Find elements whose sibling index is equal to the supplied index.
     * @param index 0-based index
     * @return elements equal to index
     */
    public fun getElementsByIndexEquals(index: Int): Elements {
        return Collector.collect(Evaluator.IndexEquals(index), this)
    }

    /**
     * Find elements that contain the specified string. The search is case-insensitive. The text may appear directly
     * in the element, or in any of its descendants.
     * @param searchText to look for in the element's text
     * @return elements that contain the string, case-insensitive.
     * @see Element.text
     */
    public fun getElementsContainingText(searchText: String): Elements {
        return Collector.collect(Evaluator.ContainsText(searchText), this)
    }

    /**
     * Find elements that directly contain the specified string. The search is case-insensitive. The text must appear directly
     * in the element, not in any of its descendants.
     * @param searchText to look for in the element's own text
     * @return elements that contain the string, case-insensitive.
     * @see Element.ownText
     */
    public fun getElementsContainingOwnText(searchText: String): Elements {
        return Collector.collect(Evaluator.ContainsOwnText(searchText), this)
    }

    /**
     * Find elements whose text matches the supplied regular expression.
     * @param regex regular expression to match text against
     * @return elements matching the supplied regular expression.
     * @see Element.text
     */
    public fun getElementsMatchingText(regex: Regex): Elements {
        return Collector.collect(Evaluator.Matches(regex), this)
    }

    /**
     * Find elements whose text matches the supplied regular expression.
     * @param regex regular expression to match text against. You can use [embedded flags](http://java.sun.com/docs/books/tutorial/essential/regex/pattern.html#embedded) (such as (?i) and (?m) to control regex options.
     * @return elements matching the supplied regular expression.
     * @see Element.text
     */
    public fun getElementsMatchingText(regex: String): Elements {
        val pattern: Regex = try {
            Regex(regex)
        } catch (e: PatternSyntaxException) {
            throw IllegalArgumentException("Pattern syntax error: $regex", e)
        }
        return getElementsMatchingText(pattern)
    }

    /**
     * Find elements whose own text matches the supplied regular expression.
     * @param regex regular expression to match text against
     * @return elements matching the supplied regular expression.
     * @see Element.ownText
     */
    public fun getElementsMatchingOwnText(regex: Regex): Elements {
        return Collector.collect(Evaluator.MatchesOwn(regex), this)
    }

    /**
     * Find elements whose own text matches the supplied regular expression.
     * @param regex regular expression to match text against. You can use [embedded flags](http://java.sun.com/docs/books/tutorial/essential/regex/pattern.html#embedded) (such as (?i) and (?m) to control regex options.
     * @return elements matching the supplied regular expression.
     * @see Element.ownText
     */
    public fun getElementsMatchingOwnText(regex: String): Elements {
        val pattern: Regex = try {
            Regex(regex)
        } catch (e: PatternSyntaxException) {
            throw IllegalArgumentException("Pattern syntax error: $regex", e)
        }
        return getElementsMatchingOwnText(pattern)
    }

    public fun getAllElements(): Elements =
        Collector.collect(Evaluator.AllElements(), this)

    /**
     * Gets the **normalized, combined text** of this element and all its children. Whitespace is normalized and
     * trimmed.
     *
     * For example, given HTML `<p>Hello  <b>there</b> now! </p>`, `p.text()` returns `"Hello there
     * now!"`
     *
     * If you do not want normalized text, use [.wholeText]. If you want just the text of this node (and not
     * children), use [.ownText]
     *
     * Note that this method returns the textual content that would be presented to a reader. The contents of data
     * nodes (such as `<script>` tags) are not considered text. Use [.data] or [.html] to retrieve
     * that content.
     *
     * @return decoded, normalized text, or empty string if none.
     * @see .wholeText
     * @see .ownText
     * @see .textNodes
     */
    public fun text(): String {
        val accum: StringBuilder = StringUtil.borrowBuilder()
        NodeTraversor.traverse(TextAccumulator(accum), this)
        return StringUtil.releaseBuilder(accum).trim()
    }

    private class TextAccumulator(accum: StringBuilder) : NodeVisitor {
        private val accum: StringBuilder

        init {
            this.accum = accum
        }

        override fun head(node: Node, depth: Int) {
            if (node is TextNode) {
                appendNormalisedText(accum, node)
            } else if (node is Element) {
                if (accum.isNotEmpty() &&
                    (node.isBlock() || node.isNode("br")) &&
                    !lastCharIsWhitespace(accum)
                ) {
                    accum.append(' ')
                }
            }
        }

        override fun tail(node: Node, depth: Int) {
            // make sure there is a space between block tags and immediately following text nodes or inline elements <div>One</div>Two should be "One Two".
            if (node is Element) {
                val next: Node? = node.nextSibling()
                if (node.isBlock() && (next is TextNode || next is Element && !next.tag.formatAsBlock()) && !lastCharIsWhitespace(
                        accum,
                    )
                ) {
                    accum.append(' ')
                }
            }
        }
    }

    /**
     * Get the non-normalized, decoded text of this element and its children, including only any newlines and spaces
     * present in the original source.
     * @return decoded, non-normalized text
     * @see .text
     * @see .wholeOwnText
     */
    public fun wholeText(): String {
        val accum: StringBuilder = StringUtil.borrowBuilder()
        NodeTraversor.traverse(
            { node, depth -> appendWholeText(node, accum) },
            this,
        )
        return StringUtil.releaseBuilder(accum)
    }

    /**
     * Get the non-normalized, decoded text of this element, **not including** any child elements, including only any
     * newlines and spaces present in the original source.
     * @return decoded, non-normalized text that is a direct child of this Element
     * @see .text
     * @see .wholeText
     * @see .ownText
     * @since 1.15.1
     */
    public fun wholeOwnText(): String {
        val accum: StringBuilder = StringUtil.borrowBuilder()
        val size = childNodeSize()
        for (i in 0 until size) {
            val node: Node = childNodes[i]
            appendWholeText(node, accum)
        }
        return StringUtil.releaseBuilder(accum)
    }

    /**
     * Gets the (normalized) text owned by this element only; does not get the combined text of all children.
     *
     *
     * For example, given HTML `<p>Hello <b>there</b> now!</p>`, `p.ownText()` returns `"Hello now!"`,
     * whereas `p.text()` returns `"Hello there now!"`.
     * Note that the text within the `b` element is not returned, as it is not a direct child of the `p` element.
     *
     * @return decoded text, or empty string if none.
     * @see .text
     * @see .textNodes
     */
    public fun ownText(): String {
        val sb: StringBuilder = StringUtil.borrowBuilder()
        ownText(sb)
        return StringUtil.releaseBuilder(sb).trim()
    }

    private fun ownText(accum: StringBuilder) {
        for (i in 0 until childNodeSize()) {
            val child: Node = childNodes[i]
            if (child is TextNode) {
                appendNormalisedText(accum, child)
            } else if (child.isNode("br") && !lastCharIsWhitespace(accum)) {
                accum.append(" ")
            }
        }
    }

    /**
     * Set the text of this element. Any existing contents (text or elements) will be cleared.
     *
     * As a special case, for `<script>` and `<style>` tags, the input text will be treated as data,
     * not visible text.
     * @param text decoded text
     * @return this element
     */
    public open fun text(text: String): Element {
        empty()
        // special case for script/style in HTML: should be data node
        val owner: Document? = ownerDocument()
        // an alternate impl would be to run through the parser
        if (owner != null && owner.parser()!!.isContentForTagData(normalName())) {
            appendChild(
                DataNode(text),
            )
        } else {
            appendChild(TextNode(text))
        }
        return this
    }

    /**
     * Checks if the current element or any of its child elements contain non-whitespace text.
     * @return `true` if the element has non-blank text content, `false` otherwise.
     */
    public fun hasText(): Boolean {
        val hasText = AtomicBoolean(false)
        filter(
            object : NodeFilter {
                override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
                    if (node is TextNode) {
                        if (!node.isBlank()) {
                            hasText.set(true)
                            return NodeFilter.FilterResult.STOP
                        }
                    }
                    return NodeFilter.FilterResult.CONTINUE
                }
            },
        )
        return hasText.get()
    }

    /**
     * Get the combined data of this element. Data is e.g. the inside of a `<script>` tag. Note that data is NOT the
     * text of the element. Use [.text] to get the text that would be visible to a user, and `data()`
     * for the contents of scripts, comments, CSS styles, etc.
     *
     * @return the data, or empty string if none
     *
     * @see .dataNodes
     */
    public fun data(): String {
        val sb: StringBuilder = StringUtil.borrowBuilder()
        traverse(
            object : NodeVisitor {
                override fun head(childNode: Node, depth: Int) {
                    when (childNode) {
                        is DataNode -> {
                            sb.append(childNode.getWholeData())
                        }

                        is Comment -> {
                            sb.append(childNode.getData())
                        }

                        is CDataNode -> {
                            // this shouldn't really happen because the html parser won't see the cdata as anything special when parsing script.
                            // but in case another type gets through.
                            sb.append(childNode.getWholeText())
                        }
                    }
                }
            },
        )
        return StringUtil.releaseBuilder(sb)
    }

    /**
     * Gets the literal value of this element's "class" attribute, which may include multiple class names, space
     * separated. (E.g. on `<div class="header gray">` returns, "`header gray`")
     * @return The literal class attribute, or **empty string** if no class attribute set.
     */
    public fun className(): String {
        return attr("class").trim()
    }

    /**
     * Get each of the element's class names. E.g. on element `<div class="header gray">`,
     * returns a set of two elements `"header", "gray"`. Note that modifications to this set are not pushed to
     * the backing `class` attribute; use the [.classNames] method to persist them.
     * @return set of classnames, empty if no class attribute
     */
    public fun classNames(): MutableSet<String> {
        val names: List<String> = ClassSplit.split(className())
        val classNames: MutableSet<String> =
            LinkedHashSet(names)
        classNames.remove("") // if classNames() was empty, would include an empty class
        return classNames
    }

    /**
     * Set the element's `class` attribute to the supplied class names.
     * @param classNames set of classes
     * @return this element, for chaining
     */
    public fun classNames(classNames: Set<String>): Element {
        if (classNames.isEmpty()) {
            attributes().remove("class")
        } else {
            attributes().put("class", StringUtil.join(classNames, " "))
        }
        return this
    }

    /**
     * Tests if this element has a class. Case-insensitive.
     * @param className name of class to check for
     * @return true if it does, false if not
     */
    // performance sensitive
    public fun hasClass(className: String): Boolean {
        if (attributes == null) return false
        val classAttr: String = attributes!!.getIgnoreCase("class")
        val len = classAttr.length
        val wantLen = className.length
        if (len == 0 || len < wantLen) {
            return false
        }

        // if both lengths are equal, only need compare the className with the attribute
        if (len == wantLen) {
            return className.equals(classAttr, ignoreCase = true)
        }

        // otherwise, scan for whitespace and compare regions (with no string or arraylist allocations)
        var inClass = false
        var start = 0
        for (i in 0 until len) {
            if (classAttr[i].isWhitespace()) {
                if (inClass) {
                    // white space ends a class name, compare it with the requested one, ignore case
                    if (i - start == wantLen && classAttr.regionMatches(
                            start,
                            className,
                            0,
                            wantLen,
                            ignoreCase = true,
                        )
                    ) {
                        return true
                    }
                    inClass = false
                }
            } else {
                if (!inClass) {
                    // we're in a class name : keep the start of the substring
                    inClass = true
                    start = i
                }
            }
        }

        // check the last entry
        return if (inClass && len - start == wantLen) {
            classAttr.regionMatches(start, className, 0, wantLen, ignoreCase = true)
        } else {
            false
        }
    }

    /**
     * Add a class name to this element's `class` attribute.
     * @param className class name to add
     * @return this element
     */
    public fun addClass(className: String): Element {
        val classes = classNames()
        classes.add(className)
        classNames(classes)
        return this
    }

    /**
     * Remove a class name from this element's `class` attribute.
     * @param className class name to remove
     * @return this element
     */
    public fun removeClass(className: String): Element {
        val classes = classNames()
        classes.remove(className)
        classNames(classes)
        return this
    }

    /**
     * Toggle a class name on this element's `class` attribute: if present, remove it; otherwise add it.
     * @param className class name to toggle
     * @return this element
     */
    public fun toggleClass(className: String): Element {
        val classes = classNames()
        if (classes.contains(className)) classes.remove(className) else classes.add(className)
        classNames(classes)
        return this
    }

    /**
     * Get the value of a form element (input, textarea, etc).
     * @return the value of the form element, or empty string if not set.
     */
    public fun value(): String {
        return if (normalName() == "textarea") text() else attr("value")
    }

    /**
     * Set the value of a form element (input, textarea, etc).
     * @param value value to set
     * @return this element (for chaining)
     */
    public fun value(value: String): Element {
        if (normalName() == "textarea") text(value) else attr("value", value)
        return this
    }

    /**
     * Get the source range (start and end positions) of the end (closing) tag for this Element. Position tracking must be
     * enabled prior to parsing the content.
     * @return the range of the closing tag for this element, if it was explicitly closed in the source. `Untracked`
     * otherwise.
     * @see com.fleeksoft.ksoup.parser.Parser.setTrackPosition
     * @see Node.sourceRange
     * @since 1.15.2
     */
    internal fun endSourceRange(): Range {
        return Range.of(this, false)
    }

    public fun shouldIndent(out: Document.OutputSettings): Boolean {
        return out.prettyPrint() && isFormatAsBlock(out) && !isInlineable(out) && !preserveWhitespace(
            _parentNode,
        )
    }

    @Throws(IOException::class)
    override fun outerHtmlHead(accum: Appendable, depth: Int, out: Document.OutputSettings) {
        if (shouldIndent(out)) {
            if (accum is StringBuilder) {
                if ((accum as StringBuilder).isNotEmpty()) indent(accum, depth, out)
            } else {
                indent(accum, depth, out)
            }
        }
        accum.append('<').append(tagName())
        if (attributes != null) attributes!!.html(accum, out)

        // selfclosing includes unknown tags, isEmpty defines tags that are always empty
        if (childNodes.isEmpty() && tag.isSelfClosing()) {
            if (out.syntax() == Document.OutputSettings.Syntax.html && tag.isEmpty) {
                accum.append(
                    '>',
                )
            } else {
                accum.append(" />") // <img> in html, <img /> in xml
            }
        } else {
            accum.append('>')
        }
    }

    @Throws(IOException::class)
    override fun outerHtmlTail(accum: Appendable, depth: Int, out: Document.OutputSettings) {
        if (!(childNodes.isEmpty() && tag.isSelfClosing())) {
            if (out.prettyPrint() && childNodes.isNotEmpty() && (
                        tag.formatAsBlock() && !preserveWhitespace(
                            _parentNode,
                        ) || out.outline() && (childNodes.size > 1 || childNodes.size == 1 && childNodes[0] is Element)
                        )
            ) {
                indent(accum, depth, out)
            }
            accum.append("</").append(tagName()).append('>')
        }
    }

    /**
     * Retrieves the element's inner HTML. E.g. on a `<div>` with one empty `<p>`, would return
     * `<p></p>`. (Whereas [.outerHtml] would return `<div><p></p></div>`.)
     *
     * @return String of HTML.
     * @see .outerHtml
     */
    public fun html(): String {
        val accum: StringBuilder = StringUtil.borrowBuilder()
        html(accum)
        val html: String = StringUtil.releaseBuilder(accum)
        return if (NodeUtils.outputSettings(this).prettyPrint()) html.trim { it <= ' ' } else html
    }

    override fun <T : Appendable> html(appendable: T): T {
        val size = childNodes.size
        for (i in 0 until size) childNodes[i].outerHtml(appendable)
        return appendable
    }

    public fun copyToThis(element: Element): Element {
        element.tag = this.tag.clone()
        element._baseUri = this._baseUri
        element.shadowChildrenRef = this.shadowChildrenRef
        element.childNodes = this.childNodes.toMutableList()
        element.attributes = this.attributes?.clone()
        element._parentNode = this._parentNode?.clone()

        return element
    }

    override fun createClone(): Node {
        val element = Element(this.tag.clone(), _baseUri)
        element.shadowChildrenRef = this.shadowChildrenRef
        element.childNodes = this.childNodes
        element.attributes = this.attributes
        return element
    }

    /**
     * Set this element's inner HTML. Clears the existing HTML first.
     * @param html HTML to parse and set into this element
     * @return this element
     * @see .append
     */
    public fun html(html: String): Element {
        empty()
        append(html)
        return this
    }

    override fun clone(): Element {
        return super.clone() as Element
    }

    override fun shallowClone(): Element {
        // simpler than implementing a clone version with no child copy
        return Element(tag, baseUri(), attributes?.clone())
    }

    protected override fun doClone(parent: Node?): Element {
        val clone = super.doClone(parent) as Element
        clone.attributes = if (attributes != null) attributes!!.clone() else null
        clone.childNodes = NodeList(clone, childNodes.size) as MutableList<Node>
        clone.childNodes.addAll(childNodes) // the children then get iterated and cloned in Node.clone
        return clone
    }

    // overrides of Node for call chaining
    override fun clearAttributes(): Element {
        if (attributes != null) {
            super.clearAttributes()
            attributes = null
        }
        return this
    }

    override fun removeAttr(attributeKey: String): Element {
        return super.removeAttr(attributeKey) as Element
    }

    override fun root(): Element {
        return super.root() as Element // probably a document, but always at least an element
    }

    override fun traverse(nodeVisitor: NodeVisitor): Element {
        return super.traverse(nodeVisitor) as Element
    }

    override fun forEachNode(action: Consumer<in Node>): Element {
        return super.forEachNode(action) as Element
    }

    /**
     * Perform the supplied action on this Element and each of its descendant Elements, during a depth-first traversal.
     * Elements may be inspected, changed, added, replaced, or removed.
     * @param action the function to perform on the element
     * @return this Element, for chaining
     * @see Node.forEachNode
     */
    public fun forEach(action: Consumer<in Element?>): Element {
        NodeTraversor.traverse(
            { node, depth -> if (node is Element) action.accept(node) },
            this,
        )
        return this
    }

    override fun filter(nodeFilter: NodeFilter): Element {
        return super.filter(nodeFilter) as Element
    }

    private class NodeList(private val owner: Element, initialCapacity: Int) :
        ChangeNotifyingArrayList<Node?>(initialCapacity) {

        override fun onContentsChanged() {
            owner.nodelistChanged()
        }
    }

    private fun isFormatAsBlock(out: Document.OutputSettings): Boolean {
        return tag.isBlock || parent() != null && parent()!!.tag()
            .formatAsBlock() || out.outline()
    }

    private fun isInlineable(out: Document.OutputSettings): Boolean {
        return if (!tag.isInline()) {
            false
        } else {
            (
                    (parent() == null || parent()!!.isBlock()) &&
                            !isEffectivelyFirst() &&
                            !out.outline() &&
                            !isNode("br")
                    )
        }
    }

    internal companion object {
        private val EmptyChildren: List<Element> = emptyList()
        private val ClassSplit: Regex = Regex("\\s+")
        private val BaseUriKey: String = Attributes.internalKey("baseUri")
        private fun searchUpForAttribute(start: Element, key: String): String {
            var el: Element? = start
            while (el != null) {
                if (el.attributes != null && el.attributes!!.hasKey(key)) return el.attributes!![key]
                el = el.parent()
            }
            return ""
        }

        private fun <E : Element?> indexInList(search: Element, elements: List<E>): Int {
            val size = elements.size
            for (i in 0 until size) {
                if (elements[i] === search) return i
            }
            return 0
        }

        private fun appendWholeText(node: Node?, accum: StringBuilder) {
            if (node is TextNode) {
                accum.append(node.getWholeText())
            } else if (node!!.isNode("br")) {
                accum.append("\n")
            }
        }

        private fun appendNormalisedText(accum: StringBuilder, textNode: TextNode) {
            val text: String = textNode.getWholeText()
            if (preserveWhitespace(textNode._parentNode) || textNode is CDataNode) {
                accum.append(text)
            } else {
                StringUtil.appendNormalisedWhitespace(
                    accum,
                    text,
                    lastCharIsWhitespace(accum),
                )
            }
        }

        fun preserveWhitespace(node: Node?): Boolean {
            // looks only at this element and five levels up, to prevent recursion & needless stack searches
            if (node is Element) {
                var el = node as Element?
                var i = 0
                do {
                    if (el!!.tag.preserveWhitespace()) return true
                    el = el.parent()
                    i++
                } while (i < 6 && el != null)
            }
            return false
        }
    }
}
