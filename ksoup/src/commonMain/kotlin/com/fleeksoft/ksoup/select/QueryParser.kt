package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.Normalizer.normalize
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.jsSupportedRegex
import com.fleeksoft.ksoup.parser.TokenQueue
import com.fleeksoft.ksoup.ported.assert
import com.fleeksoft.ksoup.select.StructuralEvaluator.ImmediateParentRun

/**
 * Parses a CSS selector into an Evaluator tree.
 */
public class QueryParser private constructor(query: String) {
    private val tq: TokenQueue
    private val query: String
    private val evals: MutableList<Evaluator> = ArrayList()

    /**
     * Parse the query
     * @return Evaluator
     */
    public fun parse(): Evaluator {
        tq.consumeWhitespace()
        if (tq.matchesAny(*Combinators)) { // if starts with a combinator, use root as elements
            evals.add(StructuralEvaluator.Root())
            combinator(tq.consume())
        } else {
            evals.add(consumeEvaluator())
        }
        while (!tq.isEmpty()) {
            // hierarchy and extras
            val seenWhite: Boolean = tq.consumeWhitespace()
            if (tq.matchesAny(*Combinators)) {
                combinator(tq.consume())
            } else if (seenWhite) {
                combinator(' ')
            } else { // E.class, E#id, E[attr] etc. AND
                evals.add(consumeEvaluator()) // take next el, #. etc off queue
            }
        }
        return if (evals.size == 1) evals[0] else CombiningEvaluator.And(evals)
    }

    private fun combinator(combinator: Char) {
        tq.consumeWhitespace()
        val subQuery = consumeSubQuery() // support multi > childs
        var rootEval: Evaluator? // the new topmost evaluator
        var currentEval: Evaluator? // the evaluator the new eval will be combined to. could be root, or rightmost or.
        val newEval: Evaluator = parse(subQuery) // the evaluator to add into target evaluator
        var replaceRightMost = false
        if (evals.size == 1) {
            currentEval = evals[0]
            rootEval = currentEval
            // make sure OR (,) has precedence:
            if (rootEval is CombiningEvaluator.Or && combinator != ',') {
                currentEval = (currentEval as CombiningEvaluator.Or).rightMostEvaluator()
                assert(
                    currentEval != null,
                    "currentEval is null", // rightMost signature can return null (if none set), but always will have one by this point
                )
                replaceRightMost = true
            }
        } else {
            currentEval = CombiningEvaluator.And(evals)
            rootEval = currentEval
        }
        evals.clear()
        when (combinator) {
            '>' -> {
                val run: ImmediateParentRun =
                    if (currentEval is ImmediateParentRun) {
                        currentEval
                    } else {
                        ImmediateParentRun(
                            currentEval!!,
                        )
                    }
                run.add(newEval)
                currentEval = run
            }

            ' ' ->
                currentEval =
                    CombiningEvaluator.And(StructuralEvaluator.Parent(currentEval!!), newEval)

            '+' ->
                currentEval =
                    CombiningEvaluator.And(
                        StructuralEvaluator.ImmediatePreviousSibling(currentEval!!),
                        newEval,
                    )

            '~' ->
                currentEval =
                    CombiningEvaluator.And(
                        StructuralEvaluator.PreviousSibling(currentEval!!),
                        newEval,
                    )

            ',' -> {
                val or: CombiningEvaluator.Or
                if (currentEval is CombiningEvaluator.Or) {
                    or = currentEval
                } else {
                    or = CombiningEvaluator.Or()
                    or.add(currentEval!!)
                }
                or.add(newEval)
                currentEval = or
            }

            else -> throw Selector.SelectorParseException("Unknown combinator '$combinator'")
        }
        if (replaceRightMost) {
            (rootEval as CombiningEvaluator.Or).replaceRightMostEvaluator(
                currentEval,
            )
        } else {
            rootEval = currentEval
        }
        evals.add(rootEval)
    }

    private fun consumeSubQuery(): String {
        val sq: StringBuilder = StringUtil.borrowBuilder()
        var seenClause = false // eat until we hit a combinator after eating something else
        while (!tq.isEmpty()) {
            if (tq.matchesAny(*Combinators)) {
                if (seenClause) break
                sq.append(tq.consume())
                continue
            }
            seenClause = true
            if (tq.matches("(")) {
                sq.append("(").append(tq.chompBalanced('(', ')')).append(")")
            } else if (tq.matches("[")) {
                sq.append("[").append(tq.chompBalanced('[', ']')).append("]")
            } else {
                sq.append(tq.consume())
            }
        }
        return StringUtil.releaseBuilder(sq)
    }

    private fun consumeEvaluator(): Evaluator {
        return if (tq.matchChomp("#")) {
            byId()
        } else if (tq.matchChomp(".")) {
            byClass()
        } else if (tq.matchesWord() ||
            tq.matches(
                "*|",
            )
        ) {
            byTag()
        } else if (tq.matches("[")) {
            byAttribute()
        } else if (tq.matchChomp("*")) {
            Evaluator.AllElements()
        } else if (tq.matchChomp(
                ":",
            )
        ) {
            parsePseudoSelector()
        } else {
            throw Selector.SelectorParseException(
                "Could not parse query '$query': unexpected token at '${tq.remainder()}'",
            )
        }
    }

    private fun parsePseudoSelector(): Evaluator {
        val pseudo: String = tq.consumeCssIdentifier()
        return when (pseudo) {
            "lt" -> Evaluator.IndexLessThan(consumeIndex())
            "gt" -> Evaluator.IndexGreaterThan(consumeIndex())
            "eq" -> Evaluator.IndexEquals(consumeIndex())
            "has" -> has()
            "is" -> `is`()
            "contains" -> contains(false)
            "containsOwn" -> contains(true)
            "containsWholeText" -> containsWholeText(false)
            "containsWholeOwnText" -> containsWholeText(true)
            "containsData" -> containsData()
            "matches" -> matches(false)
            "matchesOwn" -> matches(true)
            "matchesWholeText" -> matchesWholeText(false)
            "matchesWholeOwnText" -> matchesWholeText(true)
            "not" -> not()
            "nth-child" -> cssNthChild(backwards = false, ofType = false)
            "nth-last-child" -> cssNthChild(backwards = true, ofType = false)
            "nth-of-type" -> cssNthChild(backwards = false, ofType = true)
            "nth-last-of-type" -> cssNthChild(backwards = true, ofType = true)
            "first-child" -> Evaluator.IsFirstChild()
            "last-child" -> Evaluator.IsLastChild()
            "first-of-type" -> Evaluator.IsFirstOfType()
            "last-of-type" -> Evaluator.IsLastOfType()
            "only-child" -> Evaluator.IsOnlyChild()
            "only-of-type" -> Evaluator.IsOnlyOfType()
            "empty" -> Evaluator.IsEmpty()
            "root" -> Evaluator.IsRoot()
            "matchText" -> Evaluator.MatchText()
            else -> throw Selector.SelectorParseException(
                "Could not parse query '$query': unexpected token at '${tq.remainder()}'",
            )
        }
    }

    private fun byId(): Evaluator {
        val id: String = tq.consumeCssIdentifier()
        Validate.notEmpty(id)
        return Evaluator.Id(id)
    }

    private fun byClass(): Evaluator {
        val className: String = tq.consumeCssIdentifier()
        Validate.notEmpty(className)
        return Evaluator.Class(className.trim { it <= ' ' })
    }

    private fun byTag(): Evaluator {
        // todo - these aren't dealing perfectly with case sensitivity. For case sensitive parsers, we should also make
        // the tag in the selector case-sensitive (and also attribute names). But for now, normalize (lower-case) for
        // consistency - both the selector and the element tag
        var tagName: String = normalize(tq.consumeElementSelector())
        Validate.notEmpty(tagName)
        val eval: Evaluator

        // namespaces: wildcard match equals(tagName) or ending in ":"+tagName
        if (tagName.startsWith("*|")) {
            val plainTag = tagName.substring(2) // strip *|
            eval =
                CombiningEvaluator.Or(
                    Evaluator.Tag(plainTag),
                    Evaluator.TagEndsWith(tagName.replace("*|", ":")),
                )
        } else {
            // namespaces: if element name is "abc:def", selector must be "abc|def", so flip:
            if (tagName.contains("|")) tagName = tagName.replace("|", ":")
            eval = Evaluator.Tag(tagName)
        }
        return eval
    }

    private fun byAttribute(): Evaluator {
        val cq = TokenQueue(tq.chompBalanced('[', ']')) // content queue
        val key: String =
            cq.consumeToAny(*AttributeEvals) // eq, not, start, end, contain, match, (no val)
        Validate.notEmpty(key)
        cq.consumeWhitespace()
        val eval: Evaluator
        if (cq.isEmpty()) {
            eval =
                if (key.startsWith("^")) {
                    Evaluator.AttributeStarting(key.substring(1))
                } else if (key == "*") {
                    // any attribute
                    Evaluator.AttributeStarting("")
                } else {
                    Evaluator.Attribute(key)
                }
        } else {
            if (cq.matchChomp("=")) {
                eval = Evaluator.AttributeWithValue(key, cq.remainder())
            } else if (cq.matchChomp("!=")) {
                eval = Evaluator.AttributeWithValueNot(key, cq.remainder())
            } else if (cq.matchChomp("^=")) {
                eval = Evaluator.AttributeWithValueStarting(key, cq.remainder())
            } else if (cq.matchChomp("$=")) {
                eval = Evaluator.AttributeWithValueEnding(key, cq.remainder())
            } else if (cq.matchChomp("*=")) {
                eval = Evaluator.AttributeWithValueContaining(key, cq.remainder())
            } else if (cq.matchChomp("~=")) {
                eval = Evaluator.AttributeWithValueMatching(key, jsSupportedRegex(cq.remainder()))
            } else {
                throw Selector.SelectorParseException(
                    "Could not parse attribute query '$query': unexpected token at '${cq.remainder()}'",
                )
            }
        }
        return eval
    }

    /**
     * Create a new QueryParser.
     * @param query CSS query
     */
    init {
        Validate.notEmpty(query)
        val trimmedQuery = query.trim { it <= ' ' }
        this.query = trimmedQuery
        tq = TokenQueue(trimmedQuery)
    }

    private fun cssNthChild(
        backwards: Boolean,
        ofType: Boolean,
    ): Evaluator {
        val arg = normalize(consumeParens())

        val mAB = NTH_AB.matchEntire(arg)
        val mB = NTH_B.matchEntire(arg)
        val a: Int
        val b: Int

        when {
            "odd" == arg -> {
                a = 2
                b = 1
            }

            "even" == arg -> {
                a = 2
                b = 0
            }

            mAB != null -> {
                a = if (mAB.groups[3] != null) mAB.groups[1]!!.value.replaceFirst("^\\+", "").toInt() else 1
                b = if (mAB.groups[4] != null) mAB.groups[4]!!.value.replaceFirst("^\\+", "").toInt() else 0
            }

            mB != null -> {
                a = 0
                b = mB.groups[0]!!.value.replaceFirst("^\\+", "").toInt()
            }

            else -> {
                throw Selector.SelectorParseException(
                    "Could not parse nth-index '$arg': unexpected format",
                )
            }
        }

        return when {
            ofType ->
                if (backwards) {
                    Evaluator.IsNthLastOfType(a, b)
                } else {
                    Evaluator.IsNthOfType(
                        a,
                        b,
                    )
                }

            else -> if (backwards) Evaluator.IsNthLastChild(a, b) else Evaluator.IsNthChild(a, b)
        }
    }

    /*private fun cssNthChild(backwards: Boolean, ofType: Boolean): Evaluator {
        val arg: String = normalize(consumeParens())

        val mAB: java.util.regex.Matcher = NTH_AB.matches(arg)
        val mB: java.util.regex.Matcher = NTH_B.matches(arg)
        val a: Int
        val b: Int
        if ("odd" == arg) {
            a = 2
            b = 1
        } else if ("even" == arg) {
            a = 2
            b = 0
        } else if (mAB.matches()) {
            a = if (mAB.group(3) != null) {
                mAB.group(1).replaceFirst("^\\+".toRegex(), "")
                    .toInt()
            } else {
                1
            }
            b = if (mAB.group(4) != null) {
                mAB.group(4).replaceFirst("^\\+".toRegex(), "")
                    .toInt()
            } else {
                0
            }
        } else if (mB.matches()) {
            a = 0
            b = mB.group().replaceFirst("^\\+".toRegex(), "").toInt()
        } else {
            throw SelectorParseException("Could not parse nth-index '%s': unexpected format", arg)
        }
        val eval: Evaluator
        if (ofType) if (backwards) eval = IsNthLastOfType(a, b) else eval = IsNthOfType(a, b) else {
            if (backwards) eval = IsNthLastChild(a, b) else eval = IsNthChild(a, b)
        }
        return eval
    }*/

    private fun consumeParens(): String {
        return tq.chompBalanced('(', ')')
    }

    private fun consumeIndex(): Int {
        val index = consumeParens().trim { it <= ' ' }
        Validate.isTrue(StringUtil.isNumeric(index), "Index must be numeric")
        return index.toInt()
    }

    // pseudo selector :has(el)
    private fun has(): Evaluator {
        val subQuery = consumeParens()
        Validate.notEmpty(subQuery, ":has(selector) sub-select must not be empty")
        return StructuralEvaluator.Has(parse(subQuery))
    }

    // psuedo selector :is()
    private fun `is`(): Evaluator {
        val subQuery = consumeParens()
        Validate.notEmpty(subQuery, ":is(selector) sub-select must not be empty")
        return StructuralEvaluator.Is(parse(subQuery))
    }

    // pseudo selector :contains(text), containsOwn(text)
    private fun contains(own: Boolean): Evaluator {
        val query = if (own) ":containsOwn" else ":contains"
        val searchText: String = TokenQueue.unescape(consumeParens())
        Validate.notEmpty(searchText, "$query(text) query must not be empty")
        return if (own) Evaluator.ContainsOwnText(searchText) else Evaluator.ContainsText(searchText)
    }

    private fun containsWholeText(own: Boolean): Evaluator {
        val query = if (own) ":containsWholeOwnText" else ":containsWholeText"
        val searchText: String = TokenQueue.unescape(consumeParens())
        Validate.notEmpty(searchText, "$query(text) query must not be empty")
        return if (own) Evaluator.ContainsWholeOwnText(searchText) else Evaluator.ContainsWholeText(searchText)
    }

    // pseudo selector :containsData(data)
    private fun containsData(): Evaluator {
        val searchText: String = TokenQueue.unescape(consumeParens())
        Validate.notEmpty(searchText, ":containsData(text) query must not be empty")
        return Evaluator.ContainsData(searchText)
    }

    // :matches(regex), matchesOwn(regex)
    private fun matches(own: Boolean): Evaluator {
        val query = if (own) ":matchesOwn" else ":matches"
        val regex = consumeParens() // don't unescape, as regex bits will be escaped
        Validate.notEmpty(regex, "$query(regex) query must not be empty")
        return if (own) {
            Evaluator.MatchesOwn(jsSupportedRegex(regex))
        } else {
            Evaluator.Matches(jsSupportedRegex(regex))
        }
    }

    // :matches(regex), matchesOwn(regex)
    private fun matchesWholeText(own: Boolean): Evaluator {
        val query = if (own) ":matchesWholeOwnText" else ":matchesWholeText"
        val regex = consumeParens() // don't unescape, as regex bits will be escaped
        Validate.notEmpty(regex, "$query(regex) query must not be empty")
        return if (own) {
            Evaluator.MatchesWholeOwnText(jsSupportedRegex(regex))
        } else {
            Evaluator.MatchesWholeText(jsSupportedRegex(regex))
        }
    }

    // :not(selector)
    private operator fun not(): Evaluator {
        val subQuery = consumeParens()
        Validate.notEmpty(subQuery, ":not(selector) subselect must not be empty")
        return StructuralEvaluator.Not(parse(subQuery))
    }

    override fun toString(): String {
        return query
    }

    public companion object {
        private val Combinators: CharArray = charArrayOf(',', '>', '+', '~', ' ')
        private val AttributeEvals = arrayOf("=", "!=", "^=", "$=", "*=", "~=")

        /**
         * Parse a CSS query into an Evaluator.
         * @param query CSS query
         * @return Evaluator
         * @see Selector selector query syntax
         */
        public fun parse(query: String): Evaluator {
            return try {
                val p = QueryParser(query)
                p.parse()
            } catch (e: IllegalArgumentException) {
                throw Selector.SelectorParseException(e.message)
            }
        }

        // pseudo selectors :first-child, :last-child, :nth-child, ...
        private val NTH_AB: Regex =
            Regex(
                "(([+-])?(\\d+)?)n(\\s*([+-])?\\s*\\d+)?",
                RegexOption.IGNORE_CASE,
            )
        private val NTH_B: Regex =
            Regex("([+-])?(\\d+)")
    }
}
