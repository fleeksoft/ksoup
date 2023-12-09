package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.SerializationException
import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.ported.*
import com.fleeksoft.ksoup.select.NodeFilter
import com.fleeksoft.ksoup.select.NodeTraversor
import com.fleeksoft.ksoup.select.NodeVisitor
import okio.IOException
import kotlin.reflect.KClass

/**
 * The base, abstract Node model. Elements, Documents, Comments etc are all Node instances.
 *
 * @author Sabeeh, fleeksoft@gmail.com
 */

/**
 * Default constructor. Doesn't set up base uri, children, or attributes; use with caution.
 */
public abstract class Node protected constructor() : Cloneable<Node> {
    internal var _parentNode: Node? = null // Nodes don't always have parents
    internal var siblingIndex = 0

    /**
     * Get the node name of this node. Use for debugging purposes and not logic switching (for that, use instanceof).
     * @return node name
     */
    public abstract fun nodeName(): String

    /**
     * Get the normalized name of this node. For node types other than Element, this is the same as [.nodeName].
     * For an Element, will be the lower-cased tag name.
     * @return normalized node name
     */
    public open fun normalName(): String {
        return nodeName()
    }

    /**
     * Check if this Node has an actual Attributes object.
     */
    public abstract fun hasAttributes(): Boolean

    /**
     * Checks if this node has a parent. Nodes won't have parents if (e.g.) they are newly created and not added as a child
     * to an existing node, or if they are a [.shallowClone]. In such cases, [.parent] will return `null`.
     * @return if this node has a parent.
     */
    public fun hasParent(): Boolean {
        return _parentNode != null
    }

    /**
     * Get an attribute's value by its key. **Case insensitive**
     *
     *
     * To get an absolute URL from an attribute that may be a relative URL, prefix the key with `**abs**`,
     * which is a shortcut to the [.absUrl] method.
     *
     * E.g.:
     * <blockquote>`String url = a.attr("abs:href");`</blockquote>
     *
     * @param attributeKey The attribute key.
     * @return The attribute, or empty string if not present (to avoid nulls).
     * @see .attributes
     * @see .hasAttr
     * @see .absUrl
     */
    public open fun attr(attributeKey: String): String {
        // TODO: if attr not present then null or empty?
        if (!hasAttributes()) return EmptyString
        val value: String = attributes().getIgnoreCase(attributeKey)
        return if (value.isNotEmpty()) {
            value
        } else if (attributeKey.startsWith("abs:")) {
            absUrl(
                attributeKey.substring(
                    "abs:".length,
                ),
            )
        } else {
            ""
        }
    }

    /**
     * Get each of the element's attributes.
     * @return attributes (which implements iterable, in same order as presented in original HTML).
     */
    public abstract fun attributes(): Attributes

    /**
     * Get the number of attributes that this Node has.
     * @return the number of attributes
     */
    public fun attributesSize(): Int {
        // added so that we can test how many attributes exist without implicitly creating the Attributes object
        return if (hasAttributes()) attributes().size() else 0
    }

    /**
     * Set an attribute (key=value). If the attribute already exists, it is replaced. The attribute key comparison is
     * **case insensitive**. The key will be set with case sensitivity as set in the parser settings.
     * @param attributeKey The attribute key.
     * @param attributeValue The attribute value.
     * @return this (for chaining)
     */
    public open fun attr(
        attributeKey: String,
        attributeValue: String?,
    ): Node {
        val normalizedAttributeKey = NodeUtils.parser(this).settings()!!.normalizeAttribute(attributeKey)
        attributes().putIgnoreCase(normalizedAttributeKey, attributeValue)
        return this
    }

    /**
     * Test if this Node has an attribute. **Case insensitive**.
     * @param attributeKey The attribute key to check.
     * @return true if the attribute exists, false if not.
     */
    public open fun hasAttr(attributeKey: String): Boolean {
        if (!hasAttributes()) return false
        if (attributeKey.startsWith("abs:")) {
            val key = attributeKey.substring("abs:".length)
            if (attributes().hasKeyIgnoreCase(key) && absUrl(key).isNotEmpty()) return true
        }
        return attributes().hasKeyIgnoreCase(attributeKey)
    }

    /**
     * Remove an attribute from this node.
     * @param attributeKey The attribute to remove.
     * @return this (for chaining)
     */
    public open fun removeAttr(attributeKey: String): Node {
        if (hasAttributes()) attributes().removeIgnoreCase(attributeKey)
        return this
    }

    /**
     * Clear (remove) each of the attributes in this node.
     * @return this, for chaining
     */
    public open fun clearAttributes(): Node {
        if (hasAttributes()) {
            val it: MutableIterator<Attribute> = attributes().iterator()
            while (it.hasNext()) {
                it.next()
                it.remove()
            }
        }
        return this
    }

    /**
     * Get the base URI that applies to this node. Will return an empty string if not defined. Used to make relative links
     * absolute.
     *
     * @return base URI
     * @see .absUrl
     */
    public abstract fun baseUri(): String

    /**
     * Set the baseUri for just this node (not its descendants), if this Node tracks base URIs.
     * @param baseUri new URI
     */
    protected abstract fun doSetBaseUri(baseUri: String?)

    /**
     * Update the base URI of this node and all of its descendants.
     * @param baseUri base URI to set
     */
    public fun setBaseUri(baseUri: String) {
        doSetBaseUri(baseUri)
    }

    /**
     * Get an absolute URL from a URL attribute that may be relative (such as an `<a href>` or
     * `<img src>`).
     *
     *
     * E.g.: `String absUrl = linkEl.absUrl("href");`
     *
     *
     *
     * If the attribute value is already absolute (i.e. it starts with a protocol, like
     * `http://` or `https://` etc), and it successfully parses as a URL, the attribute is
     * returned directly. Otherwise, it is treated as a URL relative to the element's [.baseUri], and made
     * absolute using that.
     *
     *
     *
     * As an alternate, you can use the [.attr] method with the `abs:` prefix, e.g.:
     * `String absUrl = linkEl.attr("abs:href");`
     *
     *
     * @param attributeKey The attribute key
     * @return An absolute URL if one could be made, or an empty string (not null) if the attribute was missing or
     * could not be made successfully into a URL.
     * @see .attr
     *
     * @see java.net.URL.URL
     */
    public open fun absUrl(attributeKey: String): String {
        Validate.notEmpty(attributeKey)
        return if (!(hasAttributes() && attributes().hasKeyIgnoreCase(attributeKey))) {
            ""
        } else {
            StringUtil.resolve(
                baseUri(),
                attributes().getIgnoreCase(attributeKey),
            )
        }
    }

    protected abstract fun ensureChildNodes(): MutableList<Node>

    /**
     * Get a child node by its 0-based index.
     * @param index index of child node
     * @return the child node at this index.
     * @throws IndexOutOfBoundsException if the index is out of bounds.
     */
    public fun childNode(index: Int): Node {
        return ensureChildNodes()[index]
    }

    /**
     * Get this node's children. Presented as an unmodifiable list: new children can not be added, but the child nodes
     * themselves can be manipulated.
     * @return list of children. If no children, returns an empty list.
     */
    public fun childNodes(): List<Node> {
        if (childNodeSize() == 0) return EmptyNodes
        val children: List<Node> = ensureChildNodes()
        val rewrap: MutableList<Node> =
            ArrayList<Node>(children.size) // wrapped so that looping and moving will not throw a CME as the source changes
        rewrap.addAll(children)
        return rewrap // TODO: unmodified list
    }

    /**
     * Returns a deep copy of this node's children. Changes made to these nodes will not be reflected in the original
     * nodes
     * @return a deep copy of this node's children
     */
    public fun childNodesCopy(): List<Node> {
        val nodes: List<Node> = ensureChildNodes()
        val children: ArrayList<Node> = ArrayList<Node>(nodes.size)
        for (node in nodes) {
            children.add(node.clone())
        }
        return children
    }

    /**
     * Get the number of child nodes that this node holds.
     * @return the number of child nodes that this node holds.
     */
    public abstract fun childNodeSize(): Int

    protected fun childNodesAsArray(): Array<Node> {
        return ensureChildNodes().toTypedArray()
    }

    /**
     * Delete all this node's children.
     * @return this node, for chaining
     */
    public abstract fun empty(): Node?

    /**
     * Gets this node's parent node.
     * @return parent node; or null if no parent.
     * @see .hasParent
     */
    public open fun parent(): Node? = _parentNode

    /**
     * Gets this node's parent node. Not overridable by extending classes, so useful if you really just need the Node type.
     * @return parent node; or null if no parent.
     */
    public fun parentNode(): Node? = _parentNode

    /**
     * Get this node's root node; that is, its topmost ancestor. If this node is the top ancestor, returns `this`.
     * @return topmost ancestor.
     */
    public open fun root(): Node {
        var node: Node = this
        while (node._parentNode != null) node = node._parentNode!!
        return node
    }

    /**
     * Gets the Document associated with this Node.
     * @return the Document associated with this Node, or null if there is no such Document.
     */
    public fun ownerDocument(): Document? {
        val root = root()
        return if (root is Document) root else null
    }

    /**
     * Remove (delete) this node from the DOM tree. If this node has children, they are also removed. If this node is
     * an orphan, nothing happens.
     */
    public fun remove() {
        if (_parentNode != null) _parentNode!!.removeChild(this)
    }

    /**
     * Insert the specified HTML into the DOM before this node (as a preceding sibling).
     * @param html HTML to add before this node
     * @return this node, for chaining
     * @see .after
     */
    public open fun before(html: String): Node {
        addSiblingHtml(siblingIndex, html)
        return this
    }

    /**
     * Insert the specified node into the DOM before this node (as a preceding sibling).
     * @param node to add before this node
     * @return this node, for chaining
     * @see .after
     */
    public open fun before(node: Node): Node {
        Validate.notNull(_parentNode)

        // if the incoming node is a sibling of this, remove it first so siblingIndex is correct on add
        if (node._parentNode === _parentNode) node.remove()
        _parentNode!!.addChildren(siblingIndex, node)
        return this
    }

    /**
     * Insert the specified HTML into the DOM after this node (as a following sibling).
     * @param html HTML to add after this node
     * @return this node, for chaining
     * @see .before
     */
    public open fun after(html: String): Node {
        addSiblingHtml(siblingIndex + 1, html)
        return this
    }

    /**
     * Insert the specified node into the DOM after this node (as a following sibling).
     * @param node to add after this node
     * @return this node, for chaining
     * @see .before
     */
    public open fun after(node: Node): Node {
        Validate.notNull(_parentNode)

        // if the incoming node is a sibling of this, remove it first so siblingIndex is correct on add
        if (node._parentNode === _parentNode) node.remove()
        _parentNode!!.addChildren(siblingIndex + 1, node)
        return this
    }

    private fun addSiblingHtml(
        index: Int,
        html: String,
    ) {
        Validate.notNull(_parentNode)
        val context: Element? = if (parent() is Element) parent() as Element? else null
        val nodes: List<Node> =
            NodeUtils.parser(this)
                .parseFragmentInput(html, context, baseUri())
        _parentNode!!.addChildren(index, *nodes.toTypedArray())
    }

    /**
     * Wrap the supplied HTML around this node.
     *
     * @param html HTML to wrap around this node, e.g. `<div class="head"></div>`. Can be arbitrarily deep. If
     * the input HTML does not parse to a result starting with an Element, this will be a no-op.
     * @return this node, for chaining.
     */
    public open fun wrap(html: String): Node {
        Validate.notEmpty(html)

        // Parse context - parent (because wrapping), this, or null
        val context: Element =
            if (_parentNode != null && _parentNode is Element) _parentNode as Element else (if (this is Element) this else null)!!
        val wrapChildren: List<Node> =
            NodeUtils.parser(this)
                .parseFragmentInput(html, context, baseUri())
        val wrapNode =
            wrapChildren[0] as? Element // nothing to wrap with; noop
                ?: return this
        val wrap: Element = wrapNode
        val deepest: Element = getDeepChild(wrap)
        if (_parentNode != null) _parentNode!!.replaceChild(this, wrap)
        deepest.addChildren(this) // side effect of tricking wrapChildren to lose first

        // remainder (unbalanced wrap, like <div></div><p></p> -- The <p> is remainder
        if (wrapChildren.isNotEmpty()) {
            for (i in wrapChildren.indices) {
                val remainder = wrapChildren[i]
                // if no parent, this could be the wrap node, so skip
                if (wrap === remainder) continue
                if (remainder._parentNode != null) remainder._parentNode!!.removeChild(remainder)
                wrap.after(remainder)
            }
        }
        return this
    }

    /**
     * Removes this node from the DOM, and moves its children up into the node's parent. This has the effect of dropping
     * the node but keeping its children.
     *
     *
     * For example, with the input html:
     *
     *
     * `<div>One <span>Two <b>Three</b></span></div>`
     * Calling `element.unwrap()` on the `span` element will result in the html:
     *
     * `<div>One Two <b>Three</b></div>`
     * and the `"Two "` [TextNode] being returned.
     *
     * @return the first child of this node, after the node has been unwrapped. @{code Null} if the node had no children.
     * @see .remove
     * @see .wrap
     */
    public fun unwrap(): Node? {
        Validate.notNull(_parentNode)
        val firstChild = firstChild()
        _parentNode!!.addChildren(siblingIndex, *childNodesAsArray())
        this.remove()
        return firstChild
    }

    private fun getDeepChild(el: Element): Element {
        var resultEl: Element = el
        var child: Element? = resultEl.firstElementChild()
        while (child != null) {
            resultEl = child
            child = child.firstElementChild()
        }
        return resultEl
    }

    internal open fun nodelistChanged() {
        // Element overrides this to clear its shadow children elements
    }

    /**
     * Replace this node in the DOM with the supplied node.
     * @param `in` the node that will replace the existing node.
     */
    public fun replaceWith(inNode: Node) {
        Validate.notNull(_parentNode)
        _parentNode!!.replaceChild(this, inNode)
    }

    private fun replaceChild(
        out: Node,
        inNode: Node,
    ) {
        Validate.isTrue(out._parentNode === this)
        if (out === inNode) return // no-op self replacement
        if (inNode._parentNode != null) inNode._parentNode!!.removeChild(inNode)
        val index = out.siblingIndex
        ensureChildNodes()[index] = inNode
        inNode._parentNode = this
        inNode.siblingIndex = index
        out._parentNode = null
    }

    protected fun setParentNode(parentNode: Node) {
        if (this._parentNode != null) this._parentNode!!.removeChild(this)
        this._parentNode = parentNode
    }

    protected open fun removeChild(out: Node) {
        Validate.isTrue(out._parentNode === this)
        val index = out.siblingIndex
        ensureChildNodes().removeAt(index)
        reindexChildren(index)
        out._parentNode = null
    }

    public fun addChildren(vararg children: Node) {
        // most used. short circuit addChildren(int), which hits reindex children and array copy
        val nodes = ensureChildNodes()
        for (child in children) {
            reparentChild(child)
            nodes.add(child)
            child.siblingIndex = nodes.size - 1
        }
    }

    public fun addChildren(
        index: Int,
        vararg children: Node,
    ) {
        if (children.isEmpty()) {
            return
        }
        val nodes = ensureChildNodes()

        // fast path - if used as a wrap (index=0, children = child[0].parent.children - do inplace
        val firstParent = children[0].parent()
        if (firstParent != null && firstParent.childNodeSize() == children.size) {
            var sameList = true
            val firstParentNodes: List<Node> = firstParent.ensureChildNodes()
            // identity check contents to see if same
            var i = children.size
            while (i-- > 0) {
                if (children[i] !== firstParentNodes[i]) {
                    sameList = false
                    break
                }
            }
            if (sameList) { // moving, so OK to empty firstParent and short-circuit
                val wasEmpty = childNodeSize() == 0
                firstParent.empty()
                nodes.addAll(index, listOf(*children))
                i = children.size
                while (i-- > 0) {
                    children[i]._parentNode = this
                }
                if (!(wasEmpty && children[0].siblingIndex == 0)) {
                    // skip reindexing if we just moved
                    reindexChildren(index)
                }
                return
            }
        }
        for (child in children) {
            reparentChild(child)
        }
        nodes.addAll(index, listOf(*children))
        reindexChildren(index)
    }

    protected fun reparentChild(child: Node) {
        child.setParentNode(this)
    }

    private fun reindexChildren(start: Int) {
        val size = childNodeSize()
        if (size == 0) return
        val childNodes: List<Node> = ensureChildNodes()
        for (i in start until size) {
            childNodes[i].siblingIndex = i
        }
    }

    /**
     * Retrieves this node's sibling nodes. Similar to [node.parent.childNodes()][.childNodes], but does not
     * include this node (a node is not a sibling of itself).
     * @return node siblings. If the node has no parent, returns an empty list.
     */
    public fun siblingNodes(): List<Node> {
        if (_parentNode == null) return emptyList()
        val nodes: List<Node> = _parentNode!!.ensureChildNodes()
        val siblings: MutableList<Node> = ArrayList<Node>(nodes.size - 1)
        for (node in nodes) if (node !== this) siblings.add(node)
        return siblings
    }

    /**
     * Get this node's next sibling.
     * @return next sibling, or {@code null} if this is the last sibling
     */
    public fun nextSibling(): Node? {
        if (_parentNode == null) return null // root
        val siblings: List<Node> = _parentNode!!.ensureChildNodes()
        val index = siblingIndex + 1
        return if (siblings.size > index) siblings[index] else null
    }

    /**
     * Get this node's previous sibling.
     * @return the previous sibling, or @{code null} if this is the first sibling
     */
    public fun previousSibling(): Node? {
        if (_parentNode == null) return null // root
        return if (siblingIndex > 0) _parentNode!!.ensureChildNodes()[siblingIndex - 1] else null
    }

    /**
     * Get the list index of this node in its node sibling list. E.g. if this is the first node
     * sibling, returns 0.
     * @return position in node sibling list
     * @see com.fleeksoft.ksoup.nodes.Element.elementSiblingIndex
     */
    public fun siblingIndex(): Int {
        return siblingIndex
    }

    /**
     * Gets the first child node of this node, or `null` if there is none. This could be any Node type, such as an
     * Element, TextNode, Comment, etc. Use [Element.firstElementChild] to get the first Element child.
     * @return the first child node, or null if there are no children.
     * @see Element.firstElementChild
     * @see .lastChild
     */
    public fun firstChild(): Node? {
        return if (childNodeSize() == 0) null else ensureChildNodes()[0]
    }

    /**
     * Gets the last child node of this node, or `null` if there is none.
     * @return the last child node, or null if there are no children.
     * @see Element.lastElementChild
     * @see .firstChild
     */
    public fun lastChild(): Node? {
        val size = childNodeSize()
        if (size == 0) return null
        val children: List<Node> = ensureChildNodes()
        return children[size - 1]
    }

    /**
     * Perform a depth-first traversal through this node and its descendants.
     * @param nodeVisitor the visitor callbacks to perform on each node
     * @return this node, for chaining
     */
    public open fun traverse(nodeVisitor: NodeVisitor): Node {
        NodeTraversor.traverse(nodeVisitor, this)
        return this
    }

    /**
     * Perform the supplied action on this Node and each of its descendants, during a depth-first traversal. Nodes may be
     * inspected, changed, added, replaced, or removed.
     * @param action the function to perform on the node
     * @return this Node, for chaining
     * @see Element.forEach
     */
    public open fun forEachNode(action: Consumer<in Node>): Node {
        nodeStream().forEach {
            action.accept(it)
        }
        /*NodeTraversor.traverse(
            { node, depth -> action.accept(node) },
            this,
        )*/
        return this
    }

    /**
     * Perform a depth-first filtered traversal through this node and its descendants.
     * @param nodeFilter the filter callbacks to perform on each node
     * @return this node, for chaining
     */
    public open fun filter(nodeFilter: NodeFilter): Node {
        NodeTraversor.filter(nodeFilter, this)
        return this
    }

    /**
     * Returns a Stream of this Node and all of its descendant Nodes. The stream has document order.
     * @return a stream of all nodes.
     * @see Element.stream
     * @since 1.17.1
     */
    public fun nodeStream(): Sequence<Node> {
        return NodeUtils.stream(this, Node::class)
    }

    /**
     * Returns a Stream of this and descendant nodes, containing only nodes of the specified type. The stream has document
     * order.
     * @return a stream of nodes filtered by type.
     * @see Element.stream
     * @since 1.17.1
     */
    public fun <T : Node> nodeStream(type: KClass<T>): Sequence<T> {
        return NodeUtils.stream(this, type)
    }

    /**
     * Get the outer HTML of this node. For example, on a `p` element, may return `<p>Para</p>`.
     * @return outer HTML
     * @see Element.html
     * @see Element.text
     */
    public open fun outerHtml(): String {
        val accum: StringBuilder = StringUtil.borrowBuilder()
        outerHtml(accum)
        return StringUtil.releaseBuilder(accum)
    }

    public fun outerHtml(accum: Appendable) {
        NodeTraversor.traverse(OuterHtmlVisitor(accum, NodeUtils.outputSettings(this)), this)
    }

    /**
     * Get the outer HTML of this node.
     * @param accum accumulator to place HTML into
     * @throws IOException if appending to the given accumulator fails.
     */
    @Throws(IOException::class)
    internal abstract fun outerHtmlHead(
        accum: Appendable,
        depth: Int,
        out: Document.OutputSettings,
    )

    @Throws(IOException::class)
    internal abstract fun outerHtmlTail(
        accum: Appendable,
        depth: Int,
        out: Document.OutputSettings,
    )

    /**
     * Write this node and its children to the given [Appendable].
     *
     * @param appendable the [Appendable] to write to.
     * @return the supplied [Appendable], for chaining.
     */
    public open fun <T : Appendable> html(appendable: T): T {
        outerHtml(appendable)
        return appendable
    }

    /**
     * Get the source range (start and end positions) in the original input source that this node was parsed from. Position
     * tracking must be enabled prior to parsing the content. For an Element, this will be the positions of the start tag.
     * @return the range for the start of the node.
     * @see com.fleeksoft.ksoup.parser.Parser.setTrackPosition
     * @see Element.endSourceRange
     */
    internal fun sourceRange(): Range {
        return Range.of(this, true)
    }

    /** Test if this node has the supplied normal name.  */
    public fun isNode(normalName: String): Boolean {
        return normalName() == normalName
    }

    public fun isEffectivelyFirst(): Boolean {
        if (siblingIndex == 0) return true
        if (siblingIndex == 1) {
            val prev = previousSibling()
            return prev is TextNode && prev.isBlank()
        }
        return false
    }

    /**
     * Gets this node's outer HTML.
     * @return outer HTML.
     * @see .outerHtml
     */
    override fun toString(): String {
        return outerHtml()
    }

    @Throws(IOException::class)
    protected fun indent(
        accum: Appendable,
        depth: Int,
        out: Document.OutputSettings,
    ) {
        accum.append('\n')
            .append(StringUtil.padding(depth * out.indentAmount(), out.maxPaddingWidth()))
    }

    /**
     * Check if this node is the same instance of another (object identity test).
     *
     * For an node value equality check, see [.hasSameValue]
     * @param other other object to compare to
     * @return true if the content of this node is the same as the other
     * @see Node.hasSameValue
     */
    override fun equals(other: Any?): Boolean {
        // implemented just so that javadoc is clear this is an identity test
        return this === other
    }

    /**
     * Provides a hashCode for this Node, based on its object identity. Changes to the Node's content will not impact the
     * result.
     * @return an object identity based hashcode for this Node
     */
    override fun hashCode(): Int {
        // implemented so that javadoc and scanners are clear this is an identity test
        return super.hashCode()
    }

    /**
     * Check if this node has the same content as another node. A node is considered the same if its name, attributes and content match the
     * other node; particularly its position in the tree does not influence its similarity.
     * @param o other object to compare to
     * @return true if the content of this node is the same as the other
     */
    public fun hasSameValue(o: Any?): Boolean {
        if (this === o) return true
        return if (o == null || this::class != o::class) false else this.outerHtml() == (o as Node).outerHtml()
    }

    internal abstract fun createClone(): Node

    /**
     * Create a stand-alone, deep copy of this node, and all of its children. The cloned node will have no siblings or
     * parent node. As a stand-alone object, any changes made to the clone or any of its children will not impact the
     * original node.
     *
     *
     * The cloned node may be adopted into another Document or node structure using [Element.appendChild].
     * @return a stand-alone cloned node, including clones of any children
     * @see .shallowClone
     */
    override fun clone(): Node {
        val thisClone = doClone(null) // splits for orphan

        // Queue up nodes that need their children cloned (BFS).
        return applyDeepClone(thisClone)
    }

    private fun applyDeepClone(thisClone: Node): Node {
        val nodesToProcess: LinkedList<Node> = mutableListOf()
        nodesToProcess.add(thisClone)
        while (nodesToProcess.isNotEmpty()) {
            val currParent: Node = nodesToProcess.removeFirst()
            val size = currParent.childNodeSize()
            for (i in 0 until size) {
                val childNodes = currParent.ensureChildNodes()
                val childClone = childNodes[i].doClone(currParent)
                childNodes[i] = childClone
                nodesToProcess.add(childClone)
            }
        }
        return thisClone
    }

    /**
     * Create a stand-alone, shallow copy of this node. None of its children (if any) will be cloned, and it will have
     * no parent or sibling nodes.
     * @return a single independent copy of this node
     * @see .clone
     */
    internal open fun shallowClone(): Node {
        return doClone(null)
    }

    /*
     * Return a clone of the node using the given parent (which can be null).
     * Not a deep copy of children.
     */
    protected open fun doClone(parent: Node?): Node {
        val clone: Node = this.createClone()
        clone._parentNode = parent // can be null, to create an orphan split
        clone.siblingIndex = if (parent == null) 0 else siblingIndex
        // if not keeping the parent, shallowClone the ownerDocument to preserve its settings
        if (parent == null && this !is Document) {
            val doc: Document? = ownerDocument()
            if (doc != null) {
                val docClone: Document = doc.shallowClone()
                clone._parentNode = docClone
                docClone.ensureChildNodes().add(clone)
            }
        }
        return clone
    }

    private class OuterHtmlVisitor(
        accum: Appendable,
        out: Document.OutputSettings,
    ) : NodeVisitor {
        private val accum: Appendable
        private val out: Document.OutputSettings

        init {
            this.accum = accum
            this.out = out
            out.prepareEncoder()
        }

        override fun head(
            node: Node,
            depth: Int,
        ) {
            try {
                node.outerHtmlHead(accum, depth, out)
            } catch (exception: IOException) {
                throw SerializationException(exception)
            }
        }

        override fun tail(
            node: Node,
            depth: Int,
        ) {
            if (node.nodeName() != "#text") { // saves a void hit.
                try {
                    node.outerHtmlTail(accum, depth, out)
                } catch (exception: IOException) {
                    throw SerializationException(exception)
                }
            }
        }
    }

    public companion object {
        public val EmptyNodes: MutableList<Node> = mutableListOf()
        public const val EmptyString: String = ""

        /** Test if this node is not null and has the supplied normal name.  */
        public fun isNode(
            node: Node?,
            normalName: String,
        ): Boolean {
            return node != null && node.normalName() == normalName
        }
    }
}
