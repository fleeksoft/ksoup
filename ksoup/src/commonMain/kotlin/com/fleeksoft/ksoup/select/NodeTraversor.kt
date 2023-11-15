package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.select.NodeFilter.FilterResult
import com.fleeksoft.ksoup.ported.assert

/**
 * Depth-first node traversor. Use to iterate through all nodes under and including the specified root node.
 *
 *
 * This implementation does not use recursion, so a deep DOM does not risk blowing the stack.
 *
 */
object NodeTraversor {
    /**
     * Start a depth-first traverse of the root and all of its descendants.
     * @param visitor Node visitor.
     * @param root the root node point to traverse.
     */
    fun traverse(visitor: NodeVisitor, root: Node) {
        var node: Node? = root
        var depth = 0
        while (node != null) {
            val parent: Node? =
                node.parentNode() // remember parent to find nodes that get replaced in .head
            val origSize = parent?.childNodeSize() ?: 0
            val next: Node? = node.nextSibling()
            visitor.head(node, depth) // visit current node
            if (parent != null && !node.hasParent()) { // removed or replaced
                if (origSize == parent.childNodeSize()) { // replaced
                    node =
                        parent.childNode(node.siblingIndex()) // replace ditches parent but keeps sibling index
                } else { // removed
                    node = next
                    if (node == null) { // last one, go up
                        node = parent
                        depth--
                    }
                    continue // don't tail removed
                }
            }
            if (node.childNodeSize() > 0) { // descend
                node = node.childNode(0)
                depth++
            } else {
                while (true) {
                    if (node == null) {
                        throw Exception("as depth > 0, will have parent")
                    }
                    if (!(node.nextSibling() == null && depth > 0)) break
                    visitor.tail(node, depth) // when no more siblings, ascend
                    node = node.parentNode()
                    depth--
                }
                visitor.tail(node!!, depth)
                if (node === root) break
                node = node.nextSibling()
            }
        }
    }

    /**
     * Start a depth-first traverse of all elements.
     * @param visitor Node visitor.
     * @param elements Elements to filter.
     */
    fun traverse(visitor: NodeVisitor, elements: Elements) {
        Validate.notNull(visitor)
        Validate.notNull(elements)
        elements.forEach {
            traverse(visitor, it)
        }
    }

    /**
     * Start a depth-first filtering of the root and all of its descendants.
     * @param filter Node visitor.
     * @param root the root node point to traverse.
     * @return The filter result of the root node, or [FilterResult.STOP].
     */
    fun filter(filter: NodeFilter, root: Node): FilterResult {
        var node: Node? = root
        var depth = 0
        while (node != null) {
            var result: FilterResult = filter.head(node, depth)
            if (result === FilterResult.STOP) return result
            // Descend into child nodes:
            if (result === FilterResult.CONTINUE && node.childNodeSize() > 0) {
                node = node.childNode(0)
                ++depth
                continue
            }
            // No siblings, move upwards:
            while (true) {
                assert(node != null, "depth > 0, so has parent")
                if (!(node?.nextSibling() == null && depth > 0)) break
                // 'tail' current node:
                if (result == FilterResult.CONTINUE || result == FilterResult.SKIP_CHILDREN) {
                    result = filter.tail(node, depth)
                    if (result == FilterResult.STOP) return result
                }
                val prev: Node? = node // In case we need to remove it below.
                node = node?.parentNode()
                depth--
                if (result === FilterResult.REMOVE) prev?.remove() // Remove AFTER finding parent.
                result = FilterResult.CONTINUE // Parent was not pruned.
            }
            // 'tail' current node, then proceed with siblings:
            if (result === FilterResult.CONTINUE || result === FilterResult.SKIP_CHILDREN) {
                result = filter.tail(node, depth)
                if (result === FilterResult.STOP) return result
            }
            if (node === root) return result
            val prev: Node? = node // In case we need to remove it below.
            node = node?.nextSibling()
            if (result === FilterResult.REMOVE) prev?.remove() // Remove AFTER finding sibling.
        }
        // root == null?
        return FilterResult.CONTINUE
    }

    /**
     * Start a depth-first filtering of all elements.
     * @param filter Node filter.
     * @param elements Elements to filter.
     */
    fun filter(filter: NodeFilter, elements: Elements) {
        Validate.notNull(filter)
        Validate.notNull(elements)
        elements.forEach { el ->
            if (filter(filter, el) == FilterResult.STOP) return
        }
    }
}
