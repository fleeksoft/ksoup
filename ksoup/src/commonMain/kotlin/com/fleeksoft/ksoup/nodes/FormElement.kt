package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.parser.Tag
import com.fleeksoft.ksoup.select.Elements

/**
 * A HTML Form Element provides ready access to the form fields/controls that are associated with it. It also allows a
 * form to easily be submitted.
 */
class FormElement
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
    fun elements(): Elements {
        return elements
    }

    /**
     * Add a form control element to this form.
     * @param element form control to add
     * @return this form element, for chaining
     */
    fun addElement(element: Element): FormElement {
        elements.add(element)
        return this
    }

    protected override fun removeChild(out: Node) {
        super.removeChild(out)
        elements.remove(out)
    }

    /**
     * Prepare to submit this form. A Connection object is created with the request set up from the form values. This
     * Connection will inherit the settings and the cookies (etc) of the connection/session used to request this Document
     * (if any), as available in [Document.connection]
     *
     * You can then set up other options (like user-agent, timeout, cookies), then execute it.
     *
     * @return a connection prepared from the values of this form, in the same session as the one used to request it
     * @throws IllegalArgumentException if the form's absolute action URL cannot be determined. Make sure you pass the
     * document's base URI when parsing.
     */
    /*fun submit(): Connection {
        val action: String = if (hasAttr("action")) absUrl("action") else baseUri()
        Validate.notEmpty(
            action,
            "Could not determine a form action URL for submit. Ensure you set a base URI when parsing.",
        )
        val method: Connection.Method =
            if (attr("method").equalsIgnoreCase("POST")) Connection.Method.POST else Connection.Method.GET
        val owner: Document = ownerDocument()
        val connection: Connection =
            if (owner != null) owner.connection().newRequest() else Jsoup.newSession()
        return connection.url(action)
            .data(formData())
            .method(method)
    }*/

    /**
     * Get the data that this form submits. The returned list is a copy of the data, and changes to the contents of the
     * list will not be reflected in the DOM.
     * @return a list of key vals
     */
    /*fun formData(): List<Connection.KeyVal> {
        val data: ArrayList<Connection.KeyVal> = ArrayList<Connection.KeyVal>()

        // iterate the form control elements and accumulate their values
        for (el in elements) {
            if (!el.tag()!!
                    .isFormSubmittable()
            ) {
                continue // contents are form listable, superset of submitable
            }
            if (el.hasAttr("disabled")) continue // skip disabled form inputs
            val name: String = el.attr("name")
            if (name.length == 0) continue
            val type: String = el.attr("type")
            if (type.equals("button", ignoreCase = true) || type.equals(
                    "image",
                    ignoreCase = true,
                )
            ) {
                continue // browsers don't submit these
            }
            if ("select" == el.normalName()) {
                val options: Elements = el.select("option[selected]")
                var set = false
                for (option in options) {
                    data.add(HttpConnection.KeyVal.create(name, option.value()))
                    set = true
                }
                if (!set) {
                    val option: Element = el.selectFirst("option")
                    if (option != null) data.add(HttpConnection.KeyVal.create(name, option.value()))
                }
            } else if ("checkbox".equals(type, ignoreCase = true) || "radio".equals(
                    type,
                    ignoreCase = true,
                )
            ) {
                // only add checkbox or radio if they have the checked attribute
                if (el.hasAttr("checked")) {
                    val value = if (el.value().length() > 0) el.value() else "on"
                    data.add(HttpConnection.KeyVal.create(name, value))
                }
            } else {
                data.add(HttpConnection.KeyVal.create(name, el.value()))
            }
        }
        return data
    }*/

    override fun clone(): FormElement {
        return super.clone() as FormElement
    }
}
