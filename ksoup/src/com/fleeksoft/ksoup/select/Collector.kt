package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.nodes.Element

/**
 * Collects a list of elements that match the supplied criteria.
 *
 * @author Sabeeh
 */
internal object Collector {
    /**
     * Build a list of elements, by visiting root and every descendant of root, and testing it against the evaluator.
     * @param eval Evaluator to test elements against
     * @param root root of tree to descend
     * @return list of matches; empty if none
     */
    fun collect(
        eval: Evaluator,
        root: Element,
    ): Elements {
        eval.reset()
        return Elements().apply {
            addAll(root.stream().filter(eval.asPredicate(root)))
        }
    }

    /**
     * Finds the first Element that matches the Evaluator that descends from the root, and stops the query once that first
     * match is found.
     * @param eval Evaluator to test elements against
     * @param root root of tree to descend
     * @return the first match; `null` if none
     */

    fun findFirst(
        eval: Evaluator,
        root: Element,
    ): Element? {
        eval.reset()
        return root.stream().firstOrNull(eval.asPredicate(root))
    }
}
