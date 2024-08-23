package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.parser.Tag
import com.fleeksoft.ksoup.select.Elements
import com.fleeksoft.ksoup.select.QueryParser

/**
 * An HTML Form Element provides ready access to the form fields/controls that are associated with it. It also allows a
 * form to easily be submitted.
 * Create a new, standalone form element.
 *
 * @param tag        tag of this element
 * @param baseUri    the base URI
 * @param attributes initial attributes
 */
public class FormElement(tag: Tag, baseUri: String?, attributes: Attributes?) : Element(tag, baseUri, attributes) {
    private val linkedEls: Elements = Elements()

    // contains form submittable elements that were linked during the parse (and due to parse rules, may no longer be a child of this form)
    private val submittable = QueryParser.parse(StringUtil.join(SharedConstants.FormSubmitTags.toList(), ", "))

    /**
     * Get the list of form control elements associated with this form.
     * @return form controls associated with this element.
     */
    public fun elements(): Elements {
        // As elements may have been added or removed from the DOM after parse, prepare a new list that unions them:
        val els = select(submittable) // current form children
        linkedEls.forEach { linkedEl ->
            if (linkedEl.ownerDocument() != null && !els.contains(linkedEl)) {
                els.add(linkedEl); // adds previously linked elements, that weren't previously removed from the DOM
            }
        }
        return els
    }

    /**
     * Add a form control element to this form.
     * @param element form control to add
     * @return this form element, for chaining
     */
    public fun addElement(element: Element): FormElement {
        linkedEls.add(element)
        return this
    }

    protected override fun removeChild(out: Node) {
        super.removeChild(out)
        linkedEls.remove(out)
    }

    override fun clone(): FormElement {
        return super.clone() as FormElement
    }
}
