package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.TestHelper
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import kotlin.test.BeforeTest

object EvaluatorDebug {
    @BeforeTest
    fun initKsoup() {
        TestHelper.initKsoup()
    }

    /**
     * Cast an Evaluator into a pseudo Document, to help visualize the query. Quite coupled to the current impl.
     */
    private fun asDocument(eval: Evaluator): Document {
        val doc: Document = Document(null)
        doc.outputSettings().outline(true).indentAmount(2)

        val el: Element = asElement(eval)
        doc.appendChild(el)

        return doc
    }

    fun asDocument(query: String?): Document {
        val eval = QueryParser.parse(query!!)
        return asDocument(eval)
    }

    internal fun asElement(eval: Evaluator): Element {
        val el = Element(eval::class.simpleName ?: "Evaluator")
        el.attr("css", eval.toString())
        el.attr("cost", eval.cost().toString())

        if (eval is CombiningEvaluator) {
            for (inner in eval.sortedEvaluators) {
                el.appendChild(asElement(inner))
            }
        } else if (eval is StructuralEvaluator.ImmediateParentRun) {
            for (inner in eval.evaluators) {
                el.appendChild(asElement(inner))
            }
        } else if (eval is StructuralEvaluator) {
            val inner = eval.evaluator
            el.appendChild(asElement(inner))
        }

        return el
    }

    fun sexpr(query: String?): String {
        val doc: Document = asDocument(query)

        val sv = SexprVisitor()
        doc.childNode(0).traverse(sv) // skip outer #document
        return sv.result()
    }

    internal class SexprVisitor : NodeVisitor {
        private var sb: StringBuilder = StringUtil.borrowBuilder()

        override fun head(
            node: Node,
            depth: Int,
        ) {
            sb.append('(')
                .append(node.nodeName())

            if (node.childNodeSize() == 0) {
                sb
                    .append(" '")
                    .append(node.attr("css"))
                    .append("'")
            } else {
                sb.append(" ")
            }
        }

        override fun tail(
            node: Node,
            depth: Int,
        ) {
            sb.append(')')
        }

        fun result(): String {
            return StringUtil.releaseBuilder(sb)
        }
    }
}
