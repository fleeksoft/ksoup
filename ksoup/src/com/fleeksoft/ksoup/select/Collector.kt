/*
 * Kotlin port of jsoup's Collector.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.nodes.Element


/**
 * Collects a list of elements that match the supplied criteria.
 *
 */
internal object Collector {

    /**
     * Build a list of elements, by visiting the root and every descendant of root, and testing it against the Evaluator.
     * @param eval Evaluator to test elements against
     * @param root root of tree to descend
     * @return list of matches; empty if none
     */
    fun collect(eval: Evaluator, root: Element): Elements {
        return stream(eval, root).toCollection(Elements())
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
     * Finds the first Element that matches the Evaluator that descends from the root, and stops the query once that first
     * match is found.
     * @param eval Evaluator to test elements against
     * @param root root of tree to descend
     * @return the first match; `null` if none
     */

    fun findFirst(eval: Evaluator, root: Element): Element? {
        return stream(eval, root).firstOrNull()
    }
}
