package com.fleeksoft.ksoup.nodes

import kotlin.reflect.KClass

/**
 * Iterate through a Node and its tree of descendants, in document order, and returns nodes of the specified type. This
 * iterator supports structural changes to the tree during the traversal, such as [Node.remove],
 * [Node.replaceWith], [Node.wrap], etc.
 *
 * See also the [NodeTraversor][com.fleeksoft.ksoup.select.NodeTraversor] if `head` and `tail` callbacks are
 * desired for each node.
 *
 * Create a NoteIterator that will iterate the supplied node, and all of its descendants. The returned [.next]
 * type will be filtered to the input type.
 * @param start initial node
 * @param type node type to filter for
 */
public class NodeIterator<T : Node>(
    start: Node,
    // the desired node class type
    private val type: KClass<T>,
) : MutableIterator<T> {
    private var root: Node? = null // root / starting node

    private var next: T? = null // the next node to return
    private var current: Node? = null // the current (last emitted) node
    private var previous: Node? = null // the previously emitted node; used to recover from structural changes

    private var currentParent: Node? = null // the current node's parent; used to detect structural changes

    init {
        restart(start)
    }

    /**
     * Restart this Iterator from the specified start node. Will act as if it were newly constructed. Useful for e.g. to
     * save some GC if the iterator is used in a tight loop.
     * @param start the new start node.
     */
    public fun restart(start: Node) {
        if (type.isInstance(start)) {
            next = start as? T // first next() will be the start node
        }

        current = start
        previous = current
        root = previous
        currentParent = current?.parent()
    }

    public override fun hasNext(): Boolean {
        maybeFindNext()
        return next != null
    }

    public override fun next(): T {
        maybeFindNext()
        if (next == null) throw NoSuchElementException()

        val result: T = next!!
        previous = current
        current = next
        currentParent = current?.parent()
        next = null
        return result
    }

    /**
     * If next is not null, looks for and sets next. If next is null after this, we have reached the end.
     */
    private fun maybeFindNext() {
        if (next != null) return

        //  change detected (removed or replaced), redo from previous
        if (currentParent != null && current?.hasParent() != true) current = previous

        next = findNextNode()
    }

    private fun findNextNode(): T? {
        var node: Node? = current
        while (true) {
            if (node!!.childNodeSize() > 0) {
                node = node.childNode(0) // descend children
            } else if (root == node) {
                node = null // complete when all children of root are fully visited
            } else if (node.nextSibling() != null) {
                node =
                    node.nextSibling() // in a descendant with no more children; traverse
            } else {
                while (true) {
                    node = node?.parent() // pop out of descendants
                    if (node == null || root == node) return null // got back to root; complete

                    if (node.nextSibling() != null) {
                        node = node.nextSibling() // traverse
                        break
                    }
                }
            }
            if (node == null) return null // reached the end

            if (type.isInstance(node)) {
                return node as? T
            }
        }
    }

    public override fun remove() {
        current?.remove()
    }

    public companion object {
        /**
         * Create a NoteIterator that will iterate the supplied node, and all of its descendants. All node types will be
         * returned.
         * @param start initial node
         */
        public fun from(start: Node): NodeIterator<Node> {
            return NodeIterator<Node>(start, Node::class)
        }
    }
}
