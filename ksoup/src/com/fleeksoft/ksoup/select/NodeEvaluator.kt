package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.LeafNode
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.internal.Normalizer.lowerCase
import com.fleeksoft.ksoup.internal.StringUtil.normaliseWhitespace

public abstract class NodeEvaluator protected constructor() : Evaluator() {

    override fun matches(root: Element, element: Element): Boolean {
        return evaluateMatch(element)
    }

    override fun matches(root: Element, leafNode: LeafNode): Boolean {
        return evaluateMatch(leafNode)
    }

    protected abstract fun evaluateMatch(node: Node): Boolean
    
    override fun wantsNodes(): Boolean {
        return true
    }

    public class InstanceType(
        val type: kotlin.reflect.KClass<out Node>,
        val selector: String
    ) : NodeEvaluator() {

        override fun evaluateMatch(node: Node): Boolean {
            return type.isInstance(node)
        }

        override fun cost(): Int {
            return 1
        }

        override fun toString(): String {
            return "::$selector"
        }
    }

    public class ContainsValue(searchText: String) : NodeEvaluator() {
        private val searchText: String = lowerCase(normaliseWhitespace(searchText))

        override fun evaluateMatch(node: Node): Boolean {
            return lowerCase(node.nodeValue()).contains(searchText)
        }

        override fun cost(): Int {
            return 6
        }

        override fun toString(): String {
            return ":contains($searchText)"
        }
    }

    /**
     * Matches nodes with no value or only whitespace.
     */
    public class BlankValue : NodeEvaluator() {

        override fun evaluateMatch(node: Node): Boolean {
            return StringUtil.isBlank(node.nodeValue())
        }

        override fun cost(): Int {
            return 4
        }

        override fun toString(): String {
            return ":blank"
        }
    }

    public class MatchesValue(private val pattern: Regex) : NodeEvaluator() {

        override fun evaluateMatch(node: Node): Boolean {
            return pattern.find(node.nodeValue()) != null
        }

        override fun cost(): Int {
            return 8
        }

        override fun toString(): String {
            return ":matches(${pattern.pattern})"
        }
    }
}