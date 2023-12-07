package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.nodes.Element

/**
 * Base combining (and, or) evaluator.
 */
internal abstract class CombiningEvaluator internal constructor() : Evaluator() {
    // maintain original order so that #toString() is sensible
    val evaluators: ArrayList<Evaluator> = ArrayList<Evaluator>()
    val sortedEvaluators: ArrayList<Evaluator> = ArrayList<Evaluator>()
    var num = 0
    private var _cost = 0

    internal constructor(evaluators: Collection<Evaluator>) : this() {
        this.evaluators.addAll(evaluators)
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

    fun rightMostEvaluator(): Evaluator? {
        return if (num > 0) evaluators[num - 1] else null
    }

    fun replaceRightMostEvaluator(replacement: Evaluator) {
        evaluators[num - 1] = replacement
        updateEvaluators()
    }

    fun updateEvaluators() {
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
    }

    // ^ comparingInt, sortedEvaluators.sort not available in targeted version
    class And internal constructor(evaluators: Collection<Evaluator>) :
        CombiningEvaluator(evaluators) {
            internal constructor(vararg evaluators: Evaluator) : this(evaluators.toList())

            override fun matches(
                root: Element,
                element: Element,
            ): Boolean {
                for (i in 0 until num) {
                    val s: Evaluator = sortedEvaluators[i]
                    if (!s.matches(root, element)) return false
                }
                return true
            }

            override fun toString(): String {
                return StringUtil.join(evaluators, "")
            }
        }

    class Or : CombiningEvaluator {
        /**
         * Create a new Or evaluator. The initial evaluators are ANDed together and used as the first clause of the OR.
         * @param evaluators initial OR clause (these are wrapped into an AND evaluator).
         */
        internal constructor(evaluators: Collection<Evaluator>) : super() {
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

        fun add(e: Evaluator) {
            evaluators.add(e)
            updateEvaluators()
        }

        override fun matches(
            root: Element,
            element: Element,
        ): Boolean {
            for (i in 0 until num) {
                val s: Evaluator = sortedEvaluators[i]
                if (s.matches(root, element)) return true
            }
            return false
        }

        override fun toString(): String {
            return StringUtil.join(evaluators, ", ")
        }
    }
}
