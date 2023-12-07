package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.ported.assert
import com.fleeksoft.ksoup.select.NodeFilter.FilterResult

/**
Node visitor interface. Provide an implementing class to {@link NodeTraversor} or to {@link Node#traverse(NodeVisitor)}
to iterate through nodes.
<p>
This interface provides two methods, {@link #head} and {@link #tail}. The head method is called when the node is first
seen, and the tail method when all of the node's children have been visited. As an example, {@code head} can be used to
emit a start tag for a node, and {@code tail} to create the end tag. The {@code tail} method defaults to a no-op, so
the {@code head} method is the {@link FunctionalInterface}.
</p>
<p><b>Example:</b></p>
<pre><code>
doc.body().traverse { node, depth ->
 when (node) {
 is Element -> print("${node.tag()}: ${node.ownText()}");
 is DataNode -> print("Data: ${node.getWholeData()}");
 else -> print("${node.nodeName()} at depth $depth");
 }
}
</code></pre>
 */
public object NodeTraversor {
    /**
     * Start a depth-first traverse of the root and all of its descendants.
     * @param visitor Node visitor.
     * @param root the root node point to traverse.
     */
    public fun traverse(
        visitor: NodeVisitor,
        root: Node,
    ) {
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
    public fun traverse(
        visitor: NodeVisitor,
        elements: Elements,
    ) {
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
    public fun filter(
        filter: NodeFilter,
        root: Node,
    ): FilterResult {
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
    public fun filter(
        filter: NodeFilter,
        elements: Elements,
    ) {
        elements.forEach { el ->
            if (filter(filter, el) == FilterResult.STOP) return
        }
    }
}
