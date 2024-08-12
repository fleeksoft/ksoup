package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.parser.HtmlTreeBuilder
import com.fleeksoft.ksoup.parser.Parser
import kotlin.reflect.KClass

/**
 * Internal helpers for Nodes, to keep the actual node APIs relatively clean. A com.fleeksoft.ksoup internal class, so don't use it as
 * there is no contract API.
 */
internal object NodeUtils {
    /**
     * Get the output setting for this node,  or if this node has no document (or parent), retrieve the default output
     * settings
     */
    fun outputSettings(node: Node): Document.OutputSettings {
        val owner: Document? = node.ownerDocument()
        return owner?.outputSettings() ?: Document("").outputSettings()
    }

    /**
     * Get the parser that was used to make this node, or the default HTML parser if it has no parent.
     */
    fun parser(node: Node): Parser {
        val doc: Document? = node.ownerDocument()
        return doc?.parser() ?: Parser(HtmlTreeBuilder())
    }

    /** Creates a Stream, starting with the supplied node.  */
    fun <T : Node> stream(
        start: Node,
        type: KClass<T>,
    ): Sequence<T> {
        val iterator: NodeIterator<T> = NodeIterator(start, type)
        return iterator.asSequence()
    }
}
