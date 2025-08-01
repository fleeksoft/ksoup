package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.internal.QuietAppendable
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.parser.Tag
import com.fleeksoft.ksoup.ported.codePointAt
import com.fleeksoft.ksoup.select.NodeVisitor

open class Printer(
    val root: Node,
    val accum: QuietAppendable,
    val settings: Document.OutputSettings
) : NodeVisitor {
    open fun addHead(el: Element, depth: Int) {
        el.outerHtmlHead(accum, settings)
    }

    open fun addTail(el: Element, depth: Int) {
        el.outerHtmlTail(accum, settings)
    }

    open fun addText(textNode: TextNode, textOptions: Int, depth: Int) {
        val options = Entities.ForText or textOptions
        Entities.escape(accum, textNode.coreValue(), settings, options)
    }

    open fun addNode(node: LeafNode, depth: Int) {
        node.outerHtmlHead(accum, settings)
    }

    fun indent(depth: Int) {
        accum.append('\n')
            .append(StringUtil.padding(depth * settings.indentAmount(), settings.maxPaddingWidth()))
    }

    override fun head(node: Node, depth: Int) {
        if (node::class == TextNode::class) addText(node as TextNode, 0, depth) // Excludes CData; falls to addNode
        else if (node is Element) addHead(node, depth)
        else addNode(node as LeafNode, depth)
    }

    override fun tail(node: Node, depth: Int) {
        if (node is Element) {
            addTail(node, depth)
        }
    }

    /** Pretty Printer */
    open class Pretty(
        root: Node, accum: QuietAppendable, settings: Document.OutputSettings
    ) : Printer(root, accum, settings) {
        var preserveWhitespace: Boolean = false

        init {
            var node: Node? = root
            while (node != null) {
                if (tagIs(Tag.PreserveWhitespace, node)) {
                    preserveWhitespace = true
                    break
                }
                node = node.parentNode()
            }
        }

        override fun addHead(el: Element, depth: Int) {
            if (shouldIndent(el))
                indent(depth)
            super.addHead(el, depth)
            if (tagIs(Tag.PreserveWhitespace, el)) preserveWhitespace = true
        }

        override fun addTail(el: Element, depth: Int) {
            if (shouldIndent(nextNonBlank(el.firstChild())))
                indent(depth)
            super.addTail(el, depth)

            // clear the preserveWhitespace if this element is not, and there are none on the stack above
            if (preserveWhitespace && el.tag.`is`(Tag.PreserveWhitespace)) {
                var parent = el.parent()
                while (parent != null) {
                    if (parent.tag().preserveWhitespace()) return
                    parent = parent.parent()
                }
                preserveWhitespace = false
            }
        }

        override fun addNode(node: LeafNode, depth: Int) {
            if (shouldIndent(node))
                indent(depth)
            super.addNode(node, depth)
        }

        override fun addText(node: TextNode, textOptions: Int, depth: Int) {
            var options = textOptions
            if (!preserveWhitespace) {
                options = options or Entities.Normalise
                options = textTrim(node, options)

                if (!node.isBlank() && isBlockEl(node.parentNode()) && shouldIndent(node))
                    indent(depth)
            }
            super.addText(node, options, depth)
        }

        fun textTrim(node: TextNode, options: Int): Int {
            if (!isBlockEl(node.parentNode())) return options
            val prev = node.previousSibling()
            var next = node.nextSibling()
            var opts = options
            // if previous is not an inline element
            if (prev !is Element || isBlockEl(prev)) {
                // if there is no previous sib; or not a text node and should be indented
                if (prev == null || (prev !is TextNode && shouldIndent(prev))) {
                    opts = opts or Entities.TrimLeading
                }
            }

            if (next == null || next !is TextNode && shouldIndent(next)) {
                opts = opts or Entities.TrimTrailing
            } else { // trim trailing whitespace if the next non-empty TextNode has leading whitespace
                next = Printer.Pretty.nextNonBlank(next)
                if (next is TextNode && StringUtil.isWhitespace(next.nodeValue().codePointAt(0).value)) {
                    opts = opts or Entities.TrimTrailing
                }
            }

            return opts
        }

        open fun shouldIndent(node: Node?): Boolean {
            if (node == null || node == root || preserveWhitespace || isBlankText(node)) return false
            if (isBlockEl(node)) return true
            val prevSib = previousNonblank(node)
            if (isBlockEl(prevSib)) return true
            val parent = node._parentNode
            if (!isBlockEl(parent) || parent!!.tag().`is`(Tag.InlineContainer) || !hasNonTextNodes(parent))
                return false
            return prevSib == null || (
                    prevSib !is TextNode &&
                            (isBlockEl(prevSib) || prevSib !is Element)
                    )
        }

        open fun isBlockEl(node: Node?): Boolean {
            if (node == null) return false
            if (node is Element) {
                val el = node
                return el.isBlock() ||
                        (!el.tag.isKnownTag() &&
                                (el.parentNode() is Document || hasChildBlocks(el)))
            }
            return false
        }

        companion object {
            const val maxScan = 5

            fun hasChildBlocks(el: Element): Boolean {
                var child = el.firstElementChild()
                var i = 0
                while (i < maxScan && child != null) {
                    if (child.isBlock() || !child.tag.isKnownTag()) return true
                    child = child.nextElementSibling()
                    i++
                }
                return false
            }

            fun hasNonTextNodes(el: Element): Boolean {
                var child = el.firstChild()
                var i = 0
                while (i < maxScan && child != null) {
                    if (child !is TextNode) return true
                    child = child.nextSibling()
                    i++
                }
                return false
            }

            fun previousNonblank(node: Node): Node? {
                var prev = node.previousSibling()
                while (isBlankText(prev)) prev = prev?.previousSibling()
                return prev
            }

            fun nextNonBlank(node: Node?): Node? {
                var n = node
                while (isBlankText(n)) n = n?.nextSibling()
                return n
            }

            fun isBlankText(node: Node?): Boolean =
                node is TextNode && node.isBlank()

            fun tagIs(option: Int, node: Node?): Boolean =
                node is Element && node.tag.`is`(option)
        }
    }

    /** Outline Printer */
    open class Outline(
        root: Node, accum: QuietAppendable, settings: Document.OutputSettings
    ) : Pretty(root, accum, settings) {
        override fun isBlockEl(node: Node?): Boolean {
            return node != null
        }

        override fun shouldIndent(node: Node?): Boolean {
            if (node == null || node == root || preserveWhitespace || isBlankText(node))
                return false
            if (node is TextNode) {
                return node.previousSibling() != null || node.nextSibling() != null
            }
            return true
        }
    }

    companion object {
        fun printerFor(root: Node, accum: QuietAppendable): Printer {
            val settings = NodeUtils.outputSettings(root)
            return when {
                settings.outline() -> Outline(root, accum, settings)
                settings.prettyPrint() -> Pretty(root, accum, settings)
                else -> Printer(root, accum, settings)
            }
        }
    }
}