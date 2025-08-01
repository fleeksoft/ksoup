/*
 * Kotlin port of jsoup's CombiningEvaluator.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.LeafNode
import kotlin.js.JsName


/**
 * Base combining (and, or) evaluator.
 */
public abstract class CombiningEvaluator internal constructor() : Evaluator() {
    // maintain original order so that #toString() is sensible
    public val evaluators: ArrayList<Evaluator> = ArrayList()
    public val sortedEvaluators: ArrayList<Evaluator> = ArrayList()
    protected var num: Int = 0
    private var _cost = 0
    @JsName("_wantsNodes")
    var wantsNodes: Boolean = false

    internal constructor(evaluators: Collection<Evaluator>) : this() {
        this.evaluators.addAll(evaluators)
        updateEvaluators()
    }

    open fun add(e: Evaluator) {
        evaluators.add(e)
        updateEvaluators()
    }

    override fun reset() {
        for (evaluator in evaluators) {
            evaluator.reset()
        }
        super.reset()
    }

    override fun cost(): Int {
        return _cost
    }

    override fun wantsNodes(): Boolean {
        return wantsNodes
    }

    public fun updateEvaluators() {
        // used so we don't need to bash on size() for every match test
        num = evaluators.size

        // sort the evaluators by lowest cost first, to optimize the evaluation order
        _cost = 0
        for (evaluator in evaluators) {
            _cost += evaluator.cost()
        }
        sortedEvaluators.clear()
        sortedEvaluators.addAll(evaluators)
        sortedEvaluators.sortWith { a, b -> a.cost() - b.cost() }


        // any want nodes?
        for (evaluator in evaluators) {
            if (evaluator.wantsNodes()) {
                wantsNodes = true
                break
            }
        }
    }

    // ^ comparingInt, sortedEvaluators.sort not available in targeted version
    public class And constructor(evaluators: Collection<Evaluator>) :
        CombiningEvaluator(evaluators) {
        constructor(vararg evaluators: Evaluator) : this(evaluators.toList())

        override fun matches(root: Element, element: Element): Boolean {
            for (i in 0..<num) {
                val eval = sortedEvaluators[i]
                if (!eval.matches(root, element)) return false
            }
            return true
        }

        public override fun matches(root: Element, leafNode: LeafNode): Boolean {
            for (i in 0..<num) {
                val eval = sortedEvaluators[i]
                if (!eval.matches(root, leafNode)) return false
            }
            return true
        }

        override fun toString(): String {
            return StringUtil.join(evaluators, "")
        }
    }

    public class Or : CombiningEvaluator {
        /**
         * Create a new Or evaluator. The initial evaluators are ANDed together and used as the first clause of the OR.
         * @param evaluators initial OR clause (these are wrapped into an AND evaluator).
         */
        constructor(evaluators: Collection<Evaluator>) : super() {
            if (num > 1) {
                this.evaluators.add(And(evaluators))
            } else {
                // 0 or 1
                this.evaluators.addAll(evaluators)
            }
            updateEvaluators()
        }

        internal constructor(vararg evaluators: Evaluator) : this(evaluators.toList())
        internal constructor() : super()

        override fun matches(root: Element, element: Element): Boolean {
            for (i in 0..<num) {
                val eval = sortedEvaluators[i]
                if (eval.matches(root, element)) return true
            }
            return false
        }

        public override fun matches(root: Element, leafNode: LeafNode): Boolean {
            for (i in 0..<num) {
                val eval = sortedEvaluators[i]
                if (eval.matches(root, leafNode)) return true
            }
            return false
        }

        override fun toString(): String {
            return StringUtil.join(evaluators, ", ")
        }
    }
}
