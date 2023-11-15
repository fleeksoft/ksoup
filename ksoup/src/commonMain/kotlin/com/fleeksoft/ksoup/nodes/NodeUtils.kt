package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.parser.HtmlTreeBuilder
import com.fleeksoft.ksoup.parser.Parser

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
    fun parser(node: Node): Parser? {
        val doc: Document? = node.ownerDocument()
        return if (doc?.parser() != null) doc.parser() else Parser(HtmlTreeBuilder())
    }

    /**
     * This impl works by compiling the input xpath expression, and then evaluating it against a W3C Document converted
     * from the original com.fleeksoft.ksoup element. The original com.fleeksoft.ksoup elements are then fetched from the w3c doc user data (where we
     * stashed them during conversion). This process could potentially be optimized by transpiling the compiled xpath
     * expression to a com.fleeksoft.ksoup Evaluator when there's 1:1 support, thus saving the W3C document conversion stage.
     */
    /*fun <T : Node> selectXpath(
        xpath: String,
        el: Element,
        nodeType: KClass<T>,
    ): List<T> {
        Validate.notEmpty(xpath)
        Validate.notNull(el)
        Validate.notNull(nodeType)
        val w3c: W3CDom = W3CDom().namespaceAware(false)
        val wDoc: org.w3c.dom.Document = w3c.fromJsoup(el)
        val contextNode: org.w3c.dom.Node = w3c.contextNode(wDoc)
        val nodeList: org.w3c.dom.NodeList = w3c.selectXpath(xpath, contextNode)
        return w3c.sourceNodes(nodeList, nodeType)
    }*/
}
