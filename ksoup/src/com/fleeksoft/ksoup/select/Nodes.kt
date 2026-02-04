package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.ported.NodesIterator
import kotlin.js.ExperimentalJsExport
import com.fleeksoft.ksoup.KmpJsExport
import kotlin.js.JsExport

/**
 * A list of [Node] objects, with methods that act on every node in the list.
 *
 * Methods that [set], [remove], or [replaceAll] nodes in the list will also act on the underlying
 * [com.fleeksoft.ksoup.nodes.Document] DOM.
 *
 * If there are other bulk methods (perhaps from Elements) that would be useful here, please
 * [provide feedback](https://jsoup.org/discussion).
 *
 * @see Element.selectNodes
 * @see Element.selectNodes
 */

@OptIn(ExperimentalJsExport::class)
@KmpJsExport
open class Nodes<T : Node>(private val delegateList: MutableList<T> = mutableListOf()) :
    MutableList<T> by delegateList {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Nodes<*>) return false

        // Compare the contents of the lists
        if (this.size != other.size) return false
        for (i in this.indices) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    override fun hashCode(): Int {
        return delegateList.hashCode()
    }

    @JsExport.Ignore
    companion object {
        /**
         * Factory function to create a new Nodes instance from a collection
         */
        operator fun <T : Node> invoke(collection: Collection<T>): Nodes<T> = Nodes(collection)

        /**
         * Factory function to create a new empty Nodes instance
         */
        operator fun <T : Node> invoke(): Nodes<T> = Nodes()
    }

    @JsExport.Ignore
    constructor(initialCapacity: Int) : this(ArrayList<T>(initialCapacity))

    @JsExport.Ignore
    constructor(nodes: Collection<T>) : this(ArrayList<T>(nodes))

    @JsExport.Ignore
    constructor(vararg nodes: T) : this(ArrayList<T>(nodes.asList()))

    override fun iterator(): MutableIterator<T> {
        return NodesIterator(delegateList.iterator())
    }

    /**
     * Creates a deep copy of these nodes.
     * @return a deep copy
     */
    open fun clone(): Nodes<T> {
        val clone = Nodes<T>(size)
        for (node in this)
            @Suppress("UNCHECKED_CAST")
            clone.add(node.clone() as T)
        return clone
    }

    /**
     * Convenience method to get the Nodes as a plain ArrayList. This allows modification to the list of nodes
     * without modifying the source Document. I.e. whereas calling `nodes.remove(0)` will remove the nodes from
     * both the Nodes and the DOM, `nodes.asList().remove(0)` will remove the node from the list only.
     *
     * Each Node is still the same DOM connected Node.
     *
     * @return a new ArrayList containing the nodes in this list
     * @see Nodes
     */
    open fun asList(): ArrayList<T> {
        return ArrayList(this)
    }

    /**
     * Remove each matched node from the DOM.
     *
     * The nodes will still be retained in this list, in case further processing of them is desired.
     *
     * E.g. HTML: `<div><p>Hello</p> <p>there</p> <img></div>`
     * `doc.select("p").remove();`
     * HTML = `<div> <img></div>`
     *
     * Note that this method should not be used to clean user-submitted HTML; rather, use [com.fleeksoft.ksoup.safety.Cleaner]
     * to clean HTML.
     *
     * @return this, for chaining
     * @see Element.empty
     * @see Elements.empty
     * @see clear
     */
    open fun remove(): Nodes<T> {
        for (node in this) {
            node.remove()
        }
        return this
    }

    /**
     * Get the combined outer HTML of all matched nodes.
     *
     * @return string of all node's outer HTML.
     * @see Elements.text
     * @see Elements.html
     */
    fun outerHtml(): String {
        return this.joinToString("\n") { it.outerHtml() }
    }

    /**
     * Get the combined outer HTML of all matched nodes. Alias of [outerHtml].
     *
     * @return string of all the node's outer HTML.
     * @see Elements.text
     * @see outerHtml
     */
    override fun toString(): String {
        return outerHtml()
    }

    /**
     * Insert the supplied HTML before each matched node's outer HTML.
     *
     * @param html HTML to insert before each node
     * @return this, for chaining
     * @see Element.before
     */
    open fun before(html: String): Nodes<T> {
        for (node in this) {
            node.before(html)
        }
        return this
    }

    /**
     * Insert the supplied HTML after each matched nodes's outer HTML.
     *
     * @param html HTML to insert after each node
     * @return this, for chaining
     * @see Element.after
     */
    open fun after(html: String): Nodes<T> {
        for (node in this) {
            node.after(html)
        }
        return this
    }

    /**
     * Wrap the supplied HTML around each matched node. For example, with HTML
     * `<p><b>This</b> is <b>Jsoup</b></p>`,
     * `doc.select("b").wrap("&lt;i&gt;&lt;/i&gt;");`
     * becomes `<p><i><b>This</b></i> is <i><b>jsoup</b></i></p>`
     * @param html HTML to wrap around each node, e.g. `<div class="head"></div>`. Can be arbitrarily deep.
     * @return this (for chaining)
     * @see Element.wrap
     */
    open fun wrap(html: String): Nodes<T> {
        Validate.notEmpty(html)
        for (node in this) {
            node.wrap(html)
        }
        return this
    }

    // list-like methods
    /**
     * Get the first matched element.
     * @return The first matched element, or `null` if contents is empty.
     */
    open fun first(): T? {
        return if (isEmpty()) null else get(0)
    }

    /**
     * Get the last matched element.
     * @return The last matched element, or `null` if contents is empty.
     */
    open fun last(): T? {
        return if (isEmpty()) null else get(size - 1)
    }

    // MutableList<T> methods that update the DOM:

    /**
     * Replace the node at the specified index in this list, and in the DOM.
     *
     * @param index index of the node to replace
     * @param element node to be stored at the specified position
     * @return the old Node at this index
     */
    override operator fun set(index: Int, element: T): T {
        Validate.expectNotNull(element)
        val old = delegateList.set(index, element)
        old.replaceWith(element)
        return old
    }

    /**
     * Remove the node at the specified index in this list, and from the DOM.
     *
     * @param index the index of the node to be removed
     * @return the old node at this index
     * @see deselect
     */
    override fun removeAt(index: Int): T {
        val old = delegateList.removeAt(index)
        old.remove()
        return old
    }

    /**
     * Remove the specified node from this list, and from the DOM.
     *
     * @param element node to be removed from this list, if present
     * @return if this list contained the Node
     * @see deselect
     */
    override fun remove(element: T): Boolean {
        val index = this.indexOf(element)
        if (index == -1) {
            return false
        } else {
            removeAt(index)
            return true
        }
    }

    /**
     * Remove the node at the specified index in this list, but not from the DOM.
     *
     * @param index the index of the node to be removed
     * @return the old node at this index
     * @see remove
     */
    open fun deselect(index: Int): T {
        return delegateList.removeAt(index)
    }

    /**
     * Remove the specified node from this list, but not from the DOM.
     *
     * @param element node to be removed from this list, if present
     * @return if this list contained the Node
     * @see remove
     */
    @JsExport.Ignore
    open fun deselect(element: Any?): Boolean {
        return delegateList.remove(element)
    }

    /**
     * Removes all the nodes from this list, and each of them from the DOM.
     *
     * @see deselectAll
     */
    override fun clear() {
        remove()
        delegateList.clear()
    }

    /**
     * Like [clear], removes all the nodes from this list, but not from the DOM.
     *
     * @see clear
     */
    fun deselectAll() {
        delegateList.clear()
    }

    /**
     * Removes from this list, and from the DOM, each of the nodes that are contained in the specified collection and are
     * in this list.
     *
     * @param elements collection containing nodes to be removed from this list
     * @return `true` if nodes were removed from this list
     */
    override fun removeAll(elements: Collection<T>): Boolean {
        var anyRemoved = false
        for (element in elements) {
            anyRemoved = this.remove(element) || anyRemoved
        }
        return anyRemoved
    }

    /**
     * Retain in this list, and in the DOM, only the nodes that are in the specified collection and are in this list. In
     * other words, remove nodes from this list and the DOM any item that is in this list but not in the specified
     * collection.
     *
     * @param elements collection containing nodes to be retained in this list
     * @return `true` if nodes were removed from this list
     */
    override fun retainAll(elements: Collection<T>): Boolean {
        val toRemove = mutableListOf<T>()
        for (element in this) {
            if (!elements.contains(element)) {
                toRemove.add(element)
            }
        }
        return if (toRemove.isNotEmpty()) {
            removeAll(toRemove)
            true
        } else {
            false
        }
    }

    /**
     * Remove from the list, and from the DOM, all nodes in this list that match the given predicate.
     *
     * @param predicate a predicate which returns `true` for nodes to be removed
     * @return `true` if nodes were removed from this list
     */
    fun removeIf(predicate: (T) -> Boolean): Boolean {
        val toRemove = mutableListOf<T>()
        for (node in this) {
            if (predicate(node)) {
                toRemove.add(node)
            }
        }
        return if (toRemove.isNotEmpty()) {
            removeAll(toRemove)
            true
        } else {
            false
        }
    }

    /**
     * Replace each node in this list with the result of the operator, and update the DOM.
     *
     * @param operator the operator to apply to each node
     */
    fun replaceAll(operator: (T) -> T) {
        for (i in this.indices) {
            this[i] = operator(this[i])
        }
    }
}