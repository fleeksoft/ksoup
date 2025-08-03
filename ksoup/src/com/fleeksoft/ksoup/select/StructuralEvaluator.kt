/*
 * Kotlin port of jsoup's StructuralEvaluator.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.internal.SoftPool
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.nodes.*
import com.fleeksoft.ksoup.ported.IdentityHashMap
import com.fleeksoft.ksoup.ported.ThreadLocal
import kotlin.js.JsName

/**
 * Base structural evaluator.
 */
public abstract class StructuralEvaluator(public val evaluator: Evaluator) : Evaluator() {
    @JsName("_wantsNodes")
    var wantsNodes: Boolean = evaluator.wantsNodes() // if the evaluator requested nodes, not just elements

    override fun wantsNodes(): Boolean {
        return wantsNodes
    }

    public val threadMemo: ThreadLocal<IdentityHashMap<Node, IdentityHashMap<Node, Boolean>>> =
        ThreadLocal { IdentityHashMap() }

    /*boolean memoMatches(final Element root, final Node node) {
        Map<Node, IdentityHashMap<Node, Boolean>> rootMemo = threadMemo.get();
        Map<Node, Boolean> memo = rootMemo.computeIfAbsent(root, Functions.identityMapFunction());
        return memo.computeIfAbsent(node, key -> evaluator.matches(root, key));
    }*/

    public fun memoMatches(root: Element, element: Node): Boolean {
        val rootMemo = threadMemo.get()
        val memo = rootMemo.getOrPut(root) { IdentityHashMap() }
        return memo.getOrPut(element) { evaluator.matches(root, element) }
    }

    override fun reset() {
        threadMemo.get().clear()
        evaluator.reset()
        super.reset()
    }

    override fun matches(root: Element, element: Element): Boolean {
        return evaluateMatch(root, element)
    }

    override fun matches(root: Element, leafNode: LeafNode): Boolean {
        return evaluateMatch(root, leafNode)
    }

    abstract fun evaluateMatch(root: Element, node: Node): Boolean

    internal class Root : Evaluator() {
        override fun matches(root: Element, element: Element): Boolean {
            return root === element
        }

        override fun cost(): Int {
            return 1
        }

        override fun toString(): String {
            return ">"
        }
    }

    internal class Has(evaluator: Evaluator) : StructuralEvaluator(evaluator) {
        companion object {
            private val NodeIterPool: SoftPool<NodeIterator<Node>> = SoftPool { NodeIterator(TextNode(""), Node::class) }
        }

        private val checkSiblings = evalWantsSiblings(evaluator) // evaluating against siblings (or children)

        override fun matches(root: Element, element: Element): Boolean {
            if (checkSiblings) { // evaluating against siblings
                var sib = element.firstElementSibling()
                while (sib != null) {
                    if (sib !== element && evaluator.matches(element, sib)) { // don't match against self
                        return true
                    }
                    sib = sib.nextElementSibling()
                }
            }
            // otherwise we only want to match children (or below), and not the input element. And we want to minimize GCs so reusing the Iterator obj
            val it = NodeIterPool.borrow()
            it.restart(element)
            try {
                while (it.hasNext()) {
                    val node = it.next()
                    if (node === element) continue  // don't match self, only descendants
                    if (evaluator.matches(element, node)) return true
                }
            } finally {
                NodeIterPool.release(it)
            }
            return false
        }

        override fun evaluateMatch(root: Element, node: Node): Boolean {
            return false // unused; :has(::comment)) goes via implicit root combinator
        }

        /* Test if the :has sub-clause wants sibling elements (vs nested elements) - will be a Combining eval */
        private fun evalWantsSiblings(eval: Evaluator): Boolean {
            if (eval is CombiningEvaluator) {
                for (innerEval in eval.evaluators) {
                    if (innerEval is PreviousSibling || innerEval is ImmediatePreviousSibling) return true
                }
            }
            return false
        }

        override fun cost(): Int {
            return 10 * evaluator.cost()
        }

        override fun toString(): String {
            return ":has($evaluator)"
        }
    }

    /** Implements the :is(sub-query) pseudo-selector  */
    internal class Is(evaluator: Evaluator) : StructuralEvaluator(evaluator) {
        override fun evaluateMatch(root: Element, node: Node): Boolean {
            return evaluator.matches(root, node)
        }

        override fun cost(): Int {
            return 2 + evaluator.cost()
        }

        override fun toString(): String {
            return ":is($evaluator)"
        }
    }

    class Not(evaluator: Evaluator) : StructuralEvaluator(evaluator) {
        override fun evaluateMatch(root: Element, node: Node): Boolean {
            return !memoMatches(root, node)
        }

        override fun cost(): Int {
            return 2 + evaluator.cost()
        }

        override fun toString(): String {
            return ":not($evaluator)"
        }
    }

    /**
     * Any Ancestor (i.e., ascending parent chain.).
     */
    public class Ancestor(evaluator: Evaluator) : StructuralEvaluator(evaluator) {
        override fun evaluateMatch(root: Element, node: Node): Boolean {
            if (root === node) return false
            var parent = node.parent()
            while (parent != null) {
                if (memoMatches(root, parent)) return true
                if (parent === root) break
                parent = parent.parent()
            }
            return false
        }

        override fun cost(): Int {
            return 8 * evaluator.cost() // probably lower than has(), but still significant, depending on doc and el depth.
        }

        override fun toString(): String {
            return "$evaluator "
        }
    }

    /**
     * Holds a list of evaluators for one > two > three immediate parent matches, and the final direct evaluator under
     * test. To match, these are effectively ANDed together, starting from the last, matching up to the first.
     */
    public class ImmediateParentRun(evaluator: Evaluator) : StructuralEvaluator(evaluator) {
        public val evaluators: ArrayList<Evaluator> = ArrayList<Evaluator>()
        private var _cost = 2

        init {
            evaluators.add(evaluator)
            _cost += evaluator.cost()
        }

        public fun add(evaluator: Evaluator) {
            evaluators.add(evaluator)
            _cost += evaluator.cost()
            wantsNodes = wantsNodes or evaluator.wantsNodes()
        }

        override fun evaluateMatch(root: Element, node: Node): Boolean {
            var el: Node? = node
            if (el === root) return false // cannot match as the second eval (first parent test) would be above the root
            for (i in evaluators.indices.reversed()) {
                if (el == null) return false
                val eval: Evaluator = evaluators[i]
                if (!eval.matches(root, el)) return false
                el = el.parent()
            }
            return true
        }

        override fun cost(): Int {
            return _cost
        }

        override fun reset() {
            for (evaluator in evaluators) {
                evaluator.reset()
            }
            super.reset()
        }

        override fun toString(): String {
            return StringUtil.join(evaluators, " > ")
        }
    }

    public class PreviousSibling(evaluator: Evaluator) : StructuralEvaluator(evaluator) {
        // matches any previous sibling, so can be same in Element only or wantsNodes context
        override fun evaluateMatch(root: Element, node: Node): Boolean {
            if (root === node) return false
            var sibling = node.firstSibling()
            while (sibling != null) {
                if (sibling === node) break
                if (memoMatches(root, sibling)) return true
                sibling = sibling.nextElementSibling()
            }
            return false
        }

        override fun cost(): Int {
            return 3 * evaluator.cost()
        }

        override fun toString(): String {
            return "$evaluator ~ "
        }
    }

    internal class ImmediatePreviousSibling(evaluator: Evaluator) : StructuralEvaluator(evaluator) {
        override fun evaluateMatch(root: Element, node: Node): Boolean {
            if (root === node) return false
            val prev = if (wantsNodes) node.previousSibling() else node.previousElementSibling()
            return prev != null && memoMatches(root, prev)
        }

        override fun cost(): Int {
            return 2 + evaluator.cost()
        }

        override fun toString(): String {
            return "$evaluator + "
        }
    }
}
