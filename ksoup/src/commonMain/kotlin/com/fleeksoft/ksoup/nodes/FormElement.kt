package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.parser.Tag
import com.fleeksoft.ksoup.select.Elements

/**
 * A HTML Form Element provides ready access to the form fields/controls that are associated with it. It also allows a
 * form to easily be submitted.
 */
public class FormElement
/**
 * Create a new, standalone form element.
 *
 * @param tag        tag of this element
 * @param baseUri    the base URI
 * @param attributes initial attributes
 */
(tag: Tag, baseUri: String?, attributes: Attributes?) : Element(tag, baseUri, attributes) {
    private val elements: Elements = Elements()

    /**
     * Get the list of form control elements associated with this form.
     * @return form controls associated with this element.
     */
    public fun elements(): Elements {
        return elements
    }

    /**
     * Add a form control element to this form.
     * @param element form control to add
     * @return this form element, for chaining
     */
    public fun addElement(element: Element): FormElement {
        elements.add(element)
        return this
    }

    protected override fun removeChild(out: Node) {
        super.removeChild(out)
        elements.remove(out)
    }

    override fun clone(): FormElement {
        return super.clone() as FormElement
    }
}
