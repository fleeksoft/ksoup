/*
 * Kotlin port of jsoup's Collector.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import kotlin.reflect.KClass


/**
 * Collects a list of elements that match the supplied criteria.
 *
 */
object Collector {

    /**
     * Build a list of elements, by visiting the root and every descendant of root, and testing it against the Evaluator.
     * @param eval Evaluator to test elements against
     * @param root root of tree to descend
     * @return list of matches; empty if none
     */
    fun collect(eval: Evaluator, root: Element): Elements {
        val sequence: Sequence<Element> = if (eval.wantsNodes()) {
            streamNodes(eval, root, Element::class)
        } else {
            stream(eval, root)
        }

        val els = sequence.toCollection(Elements())
        eval.reset() // drops any held memos
        return els
    }

    /**
     * Obtain a Stream of elements by visiting the root and every descendant of root and testing it against the evaluator.
     *
     * @param evaluator Evaluator to test elements against
     * @param root root of tree to descend
     * @return A [Sequence] of matches
     */
    fun stream(evaluator: Evaluator, root: Element): Sequence<Element> {
        evaluator.reset()
        return root.stream().filter(evaluator.asPredicate(root))
    }

    /**
     * Obtain a Stream of nodes, of the specified type, by visiting the root and every descendant of root and testing it
     * against the evaluator.
     *
     * @param evaluator Evaluator to test elements against
     * @param root root of tree to descend
     * @param type the type of node to collect (e.g. Element, LeafNode, TextNode etc)
     * @return A Stream of matches
     */
    fun <T : Node> streamNodes(evaluator: Evaluator, root: Element, type: KClass<T>): Sequence<T> {
        evaluator.reset()
        return root.nodeStream(type).filter(evaluator.asNodePredicate(root))
    }

    /**
     * Finds the first Element that matches the Evaluator that descends from the root, and stops the query once that first
     * match is found.
     * @param eval Evaluator to test elements against
     * @param root root of tree to descend
     * @return the first match; `null` if none
     */

    fun findFirst(eval: Evaluator, root: Element): Element? {
        val el = stream(eval, root).firstOrNull()
        eval.reset()
        return el
    }

    /**
     * Finds the first Node that matches the Evaluator that descends from the root, and stops the query once that first
     * match is found.
     *
     * @param eval Evaluator to test elements against
     * @param root root of tree to descend
     * @param type the type of node to collect (e.g. Element, LeafNode, TextNode etc)
     * @return the first match; null if none
     */
    fun <T : Node> findFirstNode(eval: Evaluator, root: Element, type: KClass<T>): T? {
        val node = streamNodes(eval, root, type).firstOrNull()
        eval.reset()
        return node
    }

    /**
     * Build a list of nodes that match the supplied criteria, by visiting the root and every descendant of root, and
     * testing it against the Evaluator.
     *
     * @param evaluator Evaluator to test elements against
     * @param root root of tree to descend
     * @param type the type of node to collect (e.g. Element, LeafNode, TextNode etc)
     * @return list of matches; empty if none
     */
    fun <T : Node> collectNodes(evaluator: Evaluator, root: Element, type: KClass<T>): Nodes<T> {
        return streamNodes(evaluator, root, type).toCollection(Nodes())
    }
}
