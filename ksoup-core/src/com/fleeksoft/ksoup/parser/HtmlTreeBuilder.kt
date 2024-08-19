package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.Normalizer
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.nodes.*
import com.fleeksoft.ksoup.parser.HtmlTreeBuilderState.Constants.InTableFoster
import com.fleeksoft.ksoup.parser.HtmlTreeBuilderState.ForeignContent
import com.fleeksoft.ksoup.parser.Parser.Companion.NamespaceHtml
import com.fleeksoft.ksoup.ported.assert
import com.fleeksoft.ksoup.ported.io.Reader
import kotlin.jvm.JvmOverloads

/**
 * HTML Tree Builder; creates a DOM from Tokens.
 */
public open class HtmlTreeBuilder : TreeBuilder() {
    private var state: HtmlTreeBuilderState? = null // the current state
    private var originalState: HtmlTreeBuilderState? = null // original / marked state
    private var baseUriSetFromDoc = false

    private var headElement: Element? = null // the current head element

    private var formElement: FormElement? = null // the current form element

    // fragment parse root; name only copy of context. could be null even if fragment parsing
    private var contextElement: Element? = null

    private var formattingElements: ArrayList<Element?>? =
        null // active (open) formatting elements
    private var tmplInsertMode: ArrayList<HtmlTreeBuilderState>? =
        null // stack of Template Insertion modes
    private var pendingTableCharacters: MutableList<Token.Character>? =
        null // chars in table to be shifted out
    private var emptyEnd: Token.EndTag? = null // reused empty end tag
    private var framesetOk = false // if ok to go into frameset
    public var isFosterInserts: Boolean = false // if next inserts should be fostered
    public var isFragmentParsing: Boolean = false // if parsing a fragment of html
        private set

    override fun defaultSettings(): ParseSettings? {
        return ParseSettings.htmlDefault
    }

    override fun newInstance(): HtmlTreeBuilder {
        return HtmlTreeBuilder()
    }

    override fun initialiseParse(
        input: Reader,
        baseUri: String,
        parser: Parser,
    ) {
        super.initialiseParse(input, baseUri, parser)

        // this is a bit mucky. todo - probably just create new parser objects to ensure all reset.
        state = HtmlTreeBuilderState.Initial
        originalState = null
        baseUriSetFromDoc = false
        headElement = null
        formElement = null
        contextElement = null
        formattingElements = ArrayList()
        tmplInsertMode = ArrayList()
        pendingTableCharacters = ArrayList()
        emptyEnd = Token.EndTag(this)
        framesetOk = true
        isFosterInserts = false
        isFragmentParsing = false
    }

    override fun initialiseParseFragment(context: Element?) {
        // context may be null
        state = HtmlTreeBuilderState.Initial
        isFragmentParsing = true

        if (context != null) {
            val contextName = context.normalName()
            contextElement = Element(tagFor(contextName, settings), baseUri)
            if (context.ownerDocument() != null) {
                // quirks setup:
                doc.quirksMode(context.ownerDocument()!!.quirksMode())
            }

            when (contextName) {
                "title", "textarea" -> tokeniser!!.transition(TokeniserState.Rcdata)
                "iframe", "noembed", "noframes", "style", "xmp" ->
                    tokeniser!!.transition(
                        TokeniserState.Rawtext,
                    )

                "script" -> tokeniser!!.transition(TokeniserState.ScriptData)
                "plaintext" -> tokeniser!!.transition(TokeniserState.PLAINTEXT)
                "template" -> {
                    tokeniser!!.transition(TokeniserState.Data)
                    pushTemplateMode(HtmlTreeBuilderState.InTemplate)
                }

                else -> tokeniser!!.transition(TokeniserState.Data)
            }
            doc.appendChild(contextElement!!)
            push(contextElement!!)
            resetInsertionMode()

            // setup form element to nearest form on context (up ancestor chain). ensures form controls are associated
            // with form correctly
            var formSearch: Element? = context
            while (formSearch != null) {
                if (formSearch is FormElement) {
                    formElement = formSearch
                    break
                }
                formSearch = formSearch.parent()
            }
        }
    }

    override fun completeParseFragment(): List<Node> {
        return if (contextElement != null) {
            // depending on context and the input html, content may have been added outside of the root el
            // e.g. context=p, input=div, the div will have been pushed out.
            val nodes = contextElement!!.siblingNodes()
            if (nodes.isNotEmpty()) contextElement!!.insertChildren(-1, nodes)
            contextElement!!.childNodes()
        } else doc.childNodes()
    }

    public override fun process(token: Token): Boolean {
        val dispatch = if (useCurrentOrForeignInsert(token)) this.state else ForeignContent
        return dispatch!!.process(token, this)
    }

    private fun useCurrentOrForeignInsert(token: Token): Boolean {
        // https://html.spec.whatwg.org/multipage/parsing.html#tree-construction
        // If the stack of open elements is empty
        if (getStack().isEmpty()) return true
        val el: Element = currentElement()
        val ns: String = el.tag().namespace()

        // If the adjusted current node is an element in the HTML namespace
        if (NamespaceHtml == ns) return true

        // If the adjusted current node is a MathML text integration point and the token is a start tag whose tag name is neither "mglyph" nor "malignmark"
        // If the adjusted current node is a MathML text integration point and the token is a character token
        if (isMathmlTextIntegration(el)) {
            if (token.isStartTag() &&
                "mglyph" != token.asStartTag().normalName &&
                "malignmark" != token.asStartTag().normalName
            ) {
                return true
            }
            if (token.isCharacter()) return true
        }
        // If the adjusted current node is a MathML annotation-xml element and the token is a start tag whose tag name is "svg"
        if (Parser.NamespaceMathml == ns &&
            el.nameIs("annotation-xml") &&
            token.isStartTag() && "svg" == token.asStartTag().normalName
        ) {
            return true
        }

        // If the adjusted current node is an HTML integration point and the token is a start tag
        // If the adjusted current node is an HTML integration point and the token is a character token
        return if (isHtmlIntegration(el) &&
            (token.isStartTag() || token.isCharacter())
        ) {
            true
        } else {
            token.isEOF()
        }

        // If the token is an end-of-file token
    }

    public fun process(
        token: Token,
        state: HtmlTreeBuilderState,
    ): Boolean {
        return state.process(token, this)
    }

    public fun transition(state: HtmlTreeBuilderState?) {
        this.state = state
    }

    public fun state(): HtmlTreeBuilderState? {
        return state
    }

    public fun markInsertionMode() {
        originalState = state
    }

    public fun originalState(): HtmlTreeBuilderState? {
        return originalState
    }

    public fun framesetOk(framesetOk: Boolean) {
        this.framesetOk = framesetOk
    }

    public fun framesetOk(): Boolean {
        return framesetOk
    }

    public val document: Document
        get() = doc

    public fun maybeSetBaseUri(base: Element) {
        if (baseUriSetFromDoc) {
            // only listen to the first <base href> in parse
            return
        }
        val href: String = base.absUrl("href")
        if (href.isNotEmpty()) { // ignore <base target> etc
            baseUri = href
            baseUriSetFromDoc = true
            doc.setBaseUri(href) // set on the doc so doc.createElement(Tag) will get updated base, and to update all descendants
        }
    }

    public fun error(state: HtmlTreeBuilderState?) {
        if (parser.getErrors().canAddError()) {
            parser.getErrors().add(
                ParseError(
                    reader,
                    "Unexpected ${currentToken!!.tokenType()} token [$currentToken] when in state [$state]",
                ),
            )
        }
    }

    public fun createElementFor(
        startTag: Token.StartTag,
        namespace: String,
        forcePreserveCase: Boolean,
    ): Element {
        // dedupe and normalize the attributes:
        var attributes = startTag.attributes
        if (!forcePreserveCase) attributes = settings!!.normalizeAttributes(attributes)
        if (attributes != null && !attributes.isEmpty()) {
            val dupes = attributes.deduplicate(settings!!)
            if (dupes > 0) {
                error("Dropped duplicate attribute(s) in tag [${startTag.normalName}]")
            }
        }

        val tag =
            tagFor(
                startTag.tagName!!,
                namespace,
                if (forcePreserveCase) ParseSettings.preserveCase else settings,
            )

        return if ((tag.normalName() == "form")) {
            FormElement(tag, null, attributes)
        } else {
            Element(
                tag,
                null,
                attributes,
            )
        }
    }

    /** Inserts an HTML element for the given tag)  */
    public fun insertElementFor(startTag: Token.StartTag): Element {
        val el = createElementFor(startTag, NamespaceHtml, false)
        doInsertElement(el, startTag)

        // handle self-closing tags. when the spec expects an empty tag, will directly hit insertEmpty, so won't generate this fake end tag.
        if (startTag.isSelfClosing) {
            val tag = el.tag()
            if (tag.isKnownTag()) {
                if (!tag.isEmpty) tokeniser!!.error("Tag [${tag.normalName()}] cannot be self closing; not a void tag")
                // else: ok
            } else { // unknown tag: remember this is self-closing, for output
                tag.setSelfClosing()
            }

            // effectively a pop, but fiddles with the state. handles empty style, title etc which would otherwise leave us in data state
            tokeniser!!.transition(TokeniserState.Data) // handles <script />, otherwise needs breakout steps from script data
            tokeniser!!.emit(
                emptyEnd!!.reset().name(el.tagName()),
            ) // ensure we get out of whatever state we are in. emitted for yielded processing
        }

        return el
    }

    /**
     * Inserts a foreign element. Preserves the case of the tag name and of the attributes.
     */
    public fun insertForeignElementFor(
        startTag: Token.StartTag,
        namespace: String,
    ): Element {
        val el = createElementFor(startTag, namespace, true)
        doInsertElement(el, startTag)

        if (startTag.isSelfClosing) {
            el.tag().setSelfClosing() // remember this is self-closing for output
            pop()
        }

        return el
    }

    public fun insertEmptyElementFor(startTag: Token.StartTag): Element {
        val el = createElementFor(startTag, NamespaceHtml, false)
        doInsertElement(el, startTag)
        pop()
        return el
    }

    public fun insertFormElement(
        startTag: Token.StartTag,
        onStack: Boolean,
        checkTemplateStack: Boolean,
    ): FormElement {
        val el = createElementFor(startTag, NamespaceHtml, false) as FormElement

        if (checkTemplateStack) {
            if (!onStack("template")) setFormElement(el)
        } else {
            setFormElement(el)
        }

        doInsertElement(el, startTag)
        if (!onStack) pop()
        return el
    }

    /** Inserts the Element onto the stack. All element inserts must run through this method. Performs any general
     * tests on the Element before insertion.
     * @param el the Element to insert and make the current element
     * @param token the token this element was parsed from. If null, uses a zero-width current token as intrinsic insert
     */
    private fun doInsertElement(
        el: Element,
        token: Token?,
    ) {
        if (el.tag().isFormListed && formElement != null) {
            formElement!!.addElement(el) // connect form controls to their form element
        }

        // in HTML, the xmlns attribute if set must match what the parser set the tag's namespace to
        if (parser.getErrors().canAddError() && el.hasAttr("xmlns") && el.attr("xmlns") != el.tag().namespace()) {
            error("Invalid xmlns attribute [${el.attr("xmlns")}] on tag [${el.tagName()}]")
        }

        if (isFosterInserts &&
            StringUtil.inSorted(
                currentElement().normalName(),
                InTableFoster,
            )
        ) {
            insertInFosterParent(el)
        } else {
            currentElement().appendChild(el)
        }

        push(el)
    }

    public fun insertCommentNode(token: Token.Comment) {
        val node = Comment(token.getData())
        currentElement().appendChild(node)
        onNodeInserted(node)
    }

    /** Inserts the provided character token into the current element.  */
    public fun insertCharacterNode(characterToken: Token.Character) {
        // will be doc if no current element; allows for whitespace to be inserted into the doc root object (not on the stack)
        val el = currentElement()
        insertCharacterToElement(characterToken, el)
    }

    /** Inserts the provided character token into the provided element.  */
    public fun insertCharacterToElement(
        characterToken: Token.Character,
        el: Element,
    ) {
        val node: Node
        val tagName = el.normalName()
        val data: String = characterToken.data!!

        node =
            if (characterToken.isCData()) {
                CDataNode(data)
            } else if (isContentForTagData(tagName)) {
                DataNode(data)
            } else {
                TextNode(data)
            }
        el.appendChild(node) // doesn't use insertNode, because we don't foster these; and will always have a stack.
        onNodeInserted(node)
    }

    public fun onStack(el: Element): Boolean {
        return onStack(getStack(), el)
    }

    /** Checks if there is an HTML element with the given name on the stack.  */
    public fun onStack(elName: String?): Boolean {
        return getFromStack(elName) != null
    }

    /** Gets the nearest (lowest) HTML element with the given name from the stack.  */

    public fun getFromStack(elName: String?): Element? {
        val bottom: Int = getStack().size - 1
        val upper = if (bottom >= maxQueueDepth) bottom - maxQueueDepth else 0
        for (pos in bottom downTo upper) {
            val next: Element? = getStack()[pos]
            if (next?.elementIs(elName, NamespaceHtml) == true) {
                return next
            }
        }
        return null
    }

    public fun removeFromStack(el: Element): Boolean {
        for (pos in getStack().size - 1 downTo 0) {
            val next: Element = getStack()[pos]!!
            if (next === el) {
                getStack().removeAt(pos)
                onNodeClosed(el)
                return true
            }
        }
        return false
    }

    /** Pops the stack until the given HTML element is removed.  */

    public fun popStackToClose(elName: String): Element? {
        for (pos in getStack().size - 1 downTo 0) {
            val el: Element = pop()
            if (el.elementIs(elName, NamespaceHtml)) {
                return el
            }
        }
        return null
    }

    /** Pops the stack until an element with the supplied name is removed, irrespective of namespace.  */

    public fun popStackToCloseAnyNamespace(elName: String): Element? {
        for (pos in getStack().size - 1 downTo 0) {
            val el: Element = pop()
            if (el.nameIs(elName)) {
                return el
            }
        }
        return null
    }

    /** Pops the stack until one of the given HTML elements is removed.  */
    public fun popStackToClose(vararg elNames: String) { // elnames is sorted, comes from Constants
        // elnames is sorted, comes from Constants
        for (pos in getStack().size - 1 downTo 0) {
            val el: Element = pop()
            if (StringUtil.inSorted(el.normalName(), elNames) && NamespaceHtml == el.tag().namespace()) {
                break
            }
        }
    }

    public fun clearStackToTableContext() {
        clearStackToContext("table", "template")
    }

    public fun clearStackToTableBodyContext() {
        clearStackToContext("tbody", "tfoot", "thead", "template")
    }

    public fun clearStackToTableRowContext() {
        clearStackToContext("tr", "template")
    }

    /** Removes elements from the stack until one of the supplied HTML elements is removed.  */
    private fun clearStackToContext(vararg nodeNames: String) {
        for (pos in getStack().size - 1 downTo 0) {
            val next: Element? = getStack()[pos]
            if (NamespaceHtml == next?.tag()?.namespace() &&
                (StringUtil.isIn(next.normalName(), *nodeNames) || next.nameIs("html"))
            ) {
                break
            } else {
                pop()
            }
        }
    }

    public fun aboveOnStack(el: Element): Element? {
        assert(onStack(el))
        for (pos in getStack().size - 1 downTo 0) {
            val next: Element? = getStack()[pos]
            if (next === el) {
                return getStack()[pos - 1]
            }
        }
        return null
    }

    public fun insertOnStackAfter(
        after: Element,
        inEl: Element,
    ) {
        val i: Int = getStack().lastIndexOf(after)
        Validate.isTrue(i != -1)
        getStack().add(i + 1, inEl)
    }

    public fun replaceOnStack(
        out: Element,
        `in`: Element,
    ) {
        replaceInQueue(getStack(), out, `in`)
    }

    /**
     * Reset the insertion mode, by searching up the stack for an appropriate insertion mode. The stack search depth
     * is limited to [.maxQueueDepth].
     * @return true if the insertion mode was actually changed.
     */
    public fun resetInsertionMode(): Boolean {
        // https://html.spec.whatwg.org/multipage/parsing.html#the-insertion-mode
        var last = false
        val bottom: Int = getStack().size - 1
        val upper = if (bottom >= maxQueueDepth) bottom - maxQueueDepth else 0
        val origState: HtmlTreeBuilderState? = state
        if (getStack().size == 0) { // nothing left of stack, just get to body
            transition(HtmlTreeBuilderState.InBody)
        }
        LOOP@ for (pos in bottom downTo upper) {
            var node: Element? = getStack()[pos]
            if (pos == upper) {
                last = true
                if (isFragmentParsing) node = contextElement
            }
            val name = node?.normalName() ?: ""
            if (NamespaceHtml != node!!.tag().namespace()
            ) {
                continue // only looking for HTML elements here
            }
            when (name) {
                "select" -> {
                    transition(HtmlTreeBuilderState.InSelect)
                    // todo - should loop up (with some limit) and check for table or template hits
                    break@LOOP
                }

                "td", "th" ->
                    if (!last) {
                        transition(HtmlTreeBuilderState.InCell)
                        break@LOOP
                    }

                "tr" -> {
                    transition(HtmlTreeBuilderState.InRow)
                    break@LOOP
                }

                "tbody", "thead", "tfoot" -> {
                    transition(HtmlTreeBuilderState.InTableBody)
                    break@LOOP
                }

                "caption" -> {
                    transition(HtmlTreeBuilderState.InCaption)
                    break@LOOP
                }

                "colgroup" -> {
                    transition(HtmlTreeBuilderState.InColumnGroup)
                    break@LOOP
                }

                "table" -> {
                    transition(HtmlTreeBuilderState.InTable)
                    break@LOOP
                }

                "template" -> {
                    val tmplState: HtmlTreeBuilderState? = currentTemplateMode()
                    Validate.notNull(tmplState, "Bug: no template insertion mode on stack!")
                    transition(tmplState)
                    break@LOOP
                }

                "head" ->
                    if (!last) {
                        transition(HtmlTreeBuilderState.InHead)
                        break@LOOP
                    }

                "body" -> {
                    transition(HtmlTreeBuilderState.InBody)
                    break@LOOP
                }

                "frameset" -> {
                    transition(HtmlTreeBuilderState.InFrameset)
                    break@LOOP
                }

                "html" -> {
                    transition(if (headElement == null) HtmlTreeBuilderState.BeforeHead else HtmlTreeBuilderState.AfterHead)
                    break@LOOP
                }
            }
            if (last) {
                transition(HtmlTreeBuilderState.InBody)
                break
            }
        }
        return state != origState
    }

    /** Places the body back onto the stack and moves to InBody, for cases in AfterBody / AfterAfterBody when more content comes  */
    public fun resetBody() {
        if (!onStack("body")) {
            getStack().add(doc.body()) // not onNodeInserted, as already seen
        }
        transition(HtmlTreeBuilderState.InBody)
    }

    // todo: tidy up in specific scope methods
    private val specificScopeTarget = arrayOf("")

    private fun inSpecificScope(
        targetName: String,
        baseTypes: Array<String>,
        extraTypes: Array<String>?,
    ): Boolean {
        specificScopeTarget[0] = targetName
        return inSpecificScope(specificScopeTarget, baseTypes, extraTypes)
    }

    private fun inSpecificScope(
        targetNames: Array<String>,
        baseTypes: Array<String>,
        extraTypes: Array<String>?,
    ): Boolean {
        // https://html.spec.whatwg.org/multipage/parsing.html#has-an-element-in-the-specific-scope
        val bottom: Int = getStack().size - 1
        val top = if (bottom > MaxScopeSearchDepth) bottom - MaxScopeSearchDepth else 0
        // don't walk too far up the tree
        for (pos in bottom downTo top) {
            val el: Element? = getStack()[pos]
            if (el?.tag()?.namespace() != NamespaceHtml) continue
            val elName: String = el.normalName()
            if (StringUtil.inSorted(elName, targetNames)) return true
            if (StringUtil.inSorted(elName, baseTypes)) return false
            if (extraTypes != null && StringUtil.inSorted(elName, extraTypes)) return false
        }
        // Validate.fail("Should not be reachable"); // would end up false because hitting 'html' at root (basetypes)
        return false
    }

    public fun inScope(targetNames: Array<String>): Boolean {
        return inSpecificScope(targetNames, TagsSearchInScope, null)
    }

    @JvmOverloads
    public fun inScope(
        targetName: String,
        extras: Array<String>? = null,
    ): Boolean {
        return inSpecificScope(
            targetName = targetName,
            baseTypes = TagsSearchInScope,
            extraTypes = extras,
        )
        // todo: in mathml namespace: mi, mo, mn, ms, mtext annotation-xml
        // todo: in svg namespace: forignOjbect, desc, title
    }

    public fun inListItemScope(targetName: String): Boolean {
        return inScope(targetName, TagSearchList)
    }

    public fun inButtonScope(targetName: String): Boolean {
        return inScope(targetName, TagSearchButton)
    }

    public fun inTableScope(targetName: String): Boolean {
        return inSpecificScope(targetName, TagSearchTableScope, null)
    }

    public fun inSelectScope(targetName: String): Boolean {
        for (pos in getStack().size - 1 downTo 0) {
            val el: Element = getStack()[pos] ?: continue
            val elName: String = el.normalName()
            if (elName == targetName) return true
            if (!StringUtil.inSorted(elName, TagSearchSelectScope)) {
                // all elements except
                return false
            }
        }
        Validate.fail("Should not be reachable")
        return false
    }

    /** Tests if there is some element on the stack that is not in the provided set.  */
    public fun onStackNot(allowedTags: Array<String>): Boolean {
        val bottom: Int = getStack().size - 1
        val top = if (bottom > MaxScopeSearchDepth) bottom - MaxScopeSearchDepth else 0
        // don't walk too far up the tree
        for (pos in bottom downTo top) {
            val elName: String = getStack()[pos]?.normalName() ?: continue
            if (!StringUtil.inSorted(elName, allowedTags)) return true
        }
        return false
    }

    public fun setHeadElement(headElement: Element?) {
        this.headElement = headElement
    }

    public fun getHeadElement(): Element? {
        return headElement
    }

    public fun getFormElement(): FormElement? {
        return formElement
    }

    public fun setFormElement(formElement: FormElement?) {
        this.formElement = formElement
    }

    public fun resetPendingTableCharacters() {
        pendingTableCharacters?.clear()
    }

    public fun getPendingTableCharacters(): List<Token.Character>? {
        return pendingTableCharacters
    }

    public fun addPendingTableCharacters(c: Token.Character) {
        // make a clone of the token to maintain its state (as Tokens are otherwise reset)
        val clone: Token.Character = c.clone()
        pendingTableCharacters!!.add(clone)
    }

    /**
     * 13.2.6.3 Closing elements that have implied end tags
     * When the steps below require the UA to generate implied end tags, then, while the current node is a dd element, a dt element, an li element, an optgroup element, an option element, a p element, an rb element, an rp element, an rt element, or an rtc element, the UA must pop the current node off the stack of open elements.
     *
     * If a step requires the UA to generate implied end tags but lists an element to exclude from the process, then the UA must perform the above steps as if that element was not in the above list.
     *
     * When the steps below require the UA to generate all implied end tags thoroughly, then, while the current node is a caption element, a colgroup element, a dd element, a dt element, an li element, an optgroup element, an option element, a p element, an rb element, an rp element, an rt element, an rtc element, a tbody element, a td element, a tfoot element, a th element, a thead element, or a tr element, the UA must pop the current node off the stack of open elements.
     *
     * @param excludeTag If a step requires the UA to generate implied end tags but lists an element to exclude from the
     * process, then the UA must perform the above steps as if that element was not in the above list.
     */
    public fun generateImpliedEndTags(excludeTag: String?) {
        while (StringUtil.inSorted(currentElement().normalName(), TagSearchEndTags)) {
            if (excludeTag != null && currentElementIs(excludeTag)) break
            pop()
        }
    }

    /**
     * Pops HTML elements off the stack according to the implied end tag rules
     * @param thorough if we are thorough (includes table elements etc) or not
     */
    @JvmOverloads
    public fun generateImpliedEndTags(thorough: Boolean = false) {
        val search = if (thorough) TagThoroughSearchEndTags else TagSearchEndTags
        while (NamespaceHtml == currentElement().tag().namespace() &&
            StringUtil.inSorted(currentElement().normalName(), search)
        ) {
            pop()
        }
    }

    public fun closeElement(name: String) {
        generateImpliedEndTags(name)
        if (name != currentElement().normalName()) error(state())
        popStackToClose(name)
    }

    private fun lastFormattingElement(): Element? {
        return if ((formattingElements?.size ?: 0) > 0) {
            formattingElements!![formattingElements!!.size - 1]
        } else {
            null
        }
    }

    public fun positionOfElement(el: Element?): Int {
        for (i in formattingElements!!.indices) {
            if (el === formattingElements!!.get(i)) return i
        }
        return -1
    }

    public fun removeLastFormattingElement(): Element? {
        val size: Int = formattingElements?.size ?: 0
        return if (size > 0) formattingElements!!.removeAt(size - 1) else null
    }

    // active formatting elements
    public fun pushActiveFormattingElements(`in`: Element) {
        checkActiveFormattingElements(`in`)
        formattingElements!!.add(`in`)
    }

    public fun pushWithBookmark(
        `in`: Element,
        bookmark: Int,
    ) {
        checkActiveFormattingElements(`in`)
        // catch any range errors and assume bookmark is incorrect - saves a redundant range check.
        try {
            formattingElements!!.add(bookmark, `in`)
        } catch (e: IndexOutOfBoundsException) {
            formattingElements!!.add(`in`)
        }
    }

    public fun checkActiveFormattingElements(`in`: Element) {
        var numSeen = 0
        val size: Int = formattingElements!!.size - 1
        var ceil = size - maxUsedFormattingElements
        if (ceil < 0) ceil = 0
        for (pos in size downTo ceil) {
            val el: Element = formattingElements?.get(pos) ?: break // marker
            if (isSameFormattingElement(`in`, el)) numSeen++
            if (numSeen == 3) {
                formattingElements!!.removeAt(pos)
                break
            }
        }
    }

    public fun reconstructFormattingElements() {
        if (getStack().size > maxQueueDepth) return
        val last: Element? = lastFormattingElement()
        if (last == null || onStack(last)) return
        var entry: Element? = last
        val size: Int = formattingElements?.size ?: 0
        var ceil = size - maxUsedFormattingElements
        if (ceil < 0) ceil = 0
        var pos = size - 1
        var skip = false
        while (true) {
            if (pos == ceil) { // step 4. if none before, skip to 8
                skip = true
                break
            }
            entry = formattingElements?.get(--pos) // step 5. one earlier than entry
            if (entry == null || onStack(entry)) {
                // step 6 - neither marker nor on stack
                break // jump to 8, else continue back to 4
            }
        }
        while (true) {
            if (!skip) {
                // step 7: on later than entry
                entry = formattingElements?.get(++pos)
            }
            Validate.notNull(entry) // should not occur, as we break at last element

            // 8. create new element from element, 9 insert into current node, onto stack
            skip = false // can only skip increment from 4.
            val newEl = Element(tagFor(entry!!.normalName(), settings), null, entry.attributes().clone())
            doInsertElement(newEl, null)

            // 10. replace entry with new entry
            formattingElements?.set(pos, newEl)

            // 11
            if (pos == size - 1) {
                // if not last entry in list, jump to 7
                break
            }
        }
    }

    public fun clearFormattingElementsToLastMarker() {
        while (!formattingElements!!.isEmpty()) {
            removeLastFormattingElement() ?: break
        }
    }

    public fun removeFromActiveFormattingElements(el: Element) {
        for (pos in formattingElements!!.indices.reversed()) {
            val next: Element? = formattingElements?.get(pos)
            if (next === el) {
                formattingElements!!.removeAt(pos)
                break
            }
        }
    }

    public fun isInActiveFormattingElements(el: Element): Boolean {
        return onStack(formattingElements?.mapNotNull { it }?.toList() ?: emptyList(), el)
    }

    public fun getActiveFormattingElement(nodeName: String?): Element? {
        for (pos in formattingElements!!.indices.reversed()) {
            val next: Element? = formattingElements?.get(pos)
            if (next == null) {
                // scope marker
                break
            } else if (next.nameIs(nodeName)) {
                return next
            }
        }
        return null
    }

    public fun replaceActiveFormattingElement(
        out: Element,
        `in`: Element,
    ) {
        replaceInQueue(formattingElements!!, out, `in`)
    }

    public fun insertMarkerToFormattingElements() {
        formattingElements?.add(null)
    }

    public fun insertInFosterParent(inNode: Node) {
        val fosterParent: Element?
        val lastTable: Element? = getFromStack("table")
        var isLastTableParent = false
        if (lastTable != null) {
            if (lastTable.parent() != null) {
                fosterParent = lastTable.parent()
                isLastTableParent = true
            } else {
                fosterParent = aboveOnStack(lastTable)
            }
        } else { // no table == frag
            fosterParent = getStack()[0]
        }
        if (isLastTableParent) {
            Validate.notNull(lastTable) // last table cannot be null by this point.
            lastTable!!.before(inNode)
        } else {
            fosterParent!!.appendChild(inNode)
        }
    }

    // Template Insertion Mode stack
    public fun pushTemplateMode(state: HtmlTreeBuilderState) {
        tmplInsertMode?.add(state)
    }

    public fun popTemplateMode(): HtmlTreeBuilderState? {
        return if (!tmplInsertMode.isNullOrEmpty()) {
            tmplInsertMode?.removeAt(tmplInsertMode!!.size - 1)
        } else {
            null
        }
    }

    public fun templateModeSize(): Int {
        return tmplInsertMode?.size ?: 0
    }

    public fun currentTemplateMode(): HtmlTreeBuilderState? {
        return if (tmplInsertMode!!.size > 0) tmplInsertMode?.get(tmplInsertMode!!.size - 1) else null
    }

    override fun toString(): String {
        return "TreeBuilder{" +
                "currentToken=" + currentToken +
                ", state=" + state +
                ", currentElement=" + currentElement() +
                '}'
    }

    override fun isContentForTagData(normalName: String): Boolean {
        return normalName == "script" || normalName == "style"
    }

    public companion object {
        // tag searches. must be sorted, used in inSorted. HtmlTreeBuilderTest validates they're sorted.
        public val TagsSearchInScope: Array<String> =
            arrayOf("applet", "caption", "html", "marquee", "object", "table", "td", "th")
        public val TagSearchList: Array<String> = arrayOf("ol", "ul")
        public val TagSearchButton: Array<String> = arrayOf("button")
        public val TagSearchTableScope: Array<String> = arrayOf("html", "table")
        public val TagSearchSelectScope: Array<String> = arrayOf("optgroup", "option")
        public val TagSearchEndTags: Array<String> =
            arrayOf("dd", "dt", "li", "optgroup", "option", "p", "rb", "rp", "rt", "rtc")
        public val TagThoroughSearchEndTags: Array<String> = arrayOf(
            "caption",
            "colgroup",
            "dd",
            "dt",
            "li",
            "optgroup",
            "option",
            "p",
            "rb",
            "rp",
            "rt",
            "rtc",
            "tbody",
            "td",
            "tfoot",
            "th",
            "thead",
            "tr",
        )
        public val TagSearchSpecial: Array<String> = arrayOf(
            "address",
            "applet",
            "area",
            "article",
            "aside",
            "base",
            "basefont",
            "bgsound",
            "blockquote",
            "body",
            "br",
            "button",
            "caption",
            "center",
            "col",
            "colgroup",
            "command",
            "dd",
            "details",
            "dir",
            "div",
            "dl",
            "dt",
            "embed",
            "fieldset",
            "figcaption",
            "figure",
            "footer",
            "form",
            "frame",
            "frameset",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "head",
            "header",
            "hgroup",
            "hr",
            "html",
            "iframe",
            "img",
            "input",
            "isindex",
            "li",
            "link",
            "listing",
            "marquee",
            "menu",
            "meta",
            "nav",
            "noembed",
            "noframes",
            "noscript",
            "object",
            "ol",
            "p",
            "param",
            "plaintext",
            "pre",
            "script",
            "section",
            "select",
            "style",
            "summary",
            "table",
            "tbody",
            "td",
            "textarea",
            "tfoot",
            "th",
            "thead",
            "title",
            "tr",
            "ul",
            "wbr",
            "xmp",
        )
        public val TagMathMlTextIntegration: Array<String> = arrayOf("mi", "mn", "mo", "ms", "mtext")
        public val TagSvgHtmlIntegration: Array<String> = arrayOf("desc", "foreignObject", "title")
        public const val MaxScopeSearchDepth: Int =
            100 // prevents the parser bogging down in exceptionally broken pages
        private const val maxQueueDepth: Int = 256 // an arbitrary tension point between real HTML and crafted pain

        private fun onStack(
            queue: List<Element?>,
            element: Element,
        ): Boolean {
            val bottom: Int = queue.size - 1
            val upper = if (bottom >= maxQueueDepth) bottom - maxQueueDepth else 0
            for (pos in bottom downTo upper) {
                val next: Element? = queue[pos]
                if (next === element) {
                    return true
                }
            }
            return false
        }

        private const val maxUsedFormattingElements = 12 // limit how many elements get recreated

        public fun isMathmlTextIntegration(el: Element): Boolean {
            /*
            A node is a MathML text integration point if it is one of the following elements:
            A MathML mi element
            A MathML mo element
            A MathML mn element
            A MathML ms element
            A MathML mtext element
             */
            return (
                    Parser.NamespaceMathml == el.tag().namespace() &&
                            StringUtil.inSorted(el.normalName(), TagMathMlTextIntegration)
                    )
        }

        public fun isHtmlIntegration(el: Element): Boolean {
            /*
            A node is an HTML integration point if it is one of the following elements:
            A MathML annotation-xml element whose start tag token had an attribute with the name "encoding" whose value was an ASCII case-insensitive match for the string "text/html"
            A MathML annotation-xml element whose start tag token had an attribute with the name "encoding" whose value was an ASCII case-insensitive match for the string "application/xhtml+xml"
            An SVG foreignObject element
            An SVG desc element
            An SVG title element
             */
            if (Parser.NamespaceMathml == el.tag().namespace() && el.nameIs("annotation-xml")) {
                val encoding: String = Normalizer.normalize(el.attr("encoding"))
                if (encoding == "text/html" || encoding == "application/xhtml+xml") return true
            }
            return Parser.NamespaceSvg == el.tag().namespace() &&
                    StringUtil.isIn(
                        el.tagName(),
                        *TagSvgHtmlIntegration,
                    )
        }

        private fun replaceInQueue(
            queue: ArrayList<Element?>,
            out: Element,
            inEl: Element,
        ) {
            val i: Int = queue.lastIndexOf(out)
            Validate.isTrue(i != -1)
            queue[i] = inEl
        }

        public fun isSpecial(el: Element): Boolean {
            // todo: mathml's mi, mo, mn
            // todo: svg's foreigObject, desc, title
            val name: String = el.normalName()
            return StringUtil.inSorted(name, TagSearchSpecial)
        }

        private fun isSameFormattingElement(
            a: Element,
            b: Element,
        ): Boolean {
            // same if: same namespace, tag, and attributes. Element.equals only checks tag, might in future check children
            return a.normalName() == b.normalName() && // a.namespace().equals(b.namespace()) &&
                    a.attributes() == b.attributes()
            // todo: namespaces
        }
    }
}
