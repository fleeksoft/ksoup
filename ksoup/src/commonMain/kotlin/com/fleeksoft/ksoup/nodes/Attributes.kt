package com.fleeksoft.ksoup.nodes

import okio.IOException
import com.fleeksoft.ksoup.SerializationException
import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.Normalizer.lowerCase
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.parser.ParseSettings
import com.fleeksoft.ksoup.ported.Cloneable
import com.fleeksoft.ksoup.ported.Collections

/**
 * The attributes of an Element.
 *
 *
 * Attributes are treated as a map: there can be only one value associated with an attribute key/name.
 *
 *
 *
 * Attribute name and value comparisons are generally **case sensitive**. By default for HTML, attribute names are
 * normalized to lower-case on parsing. That means you should use lower-case strings when referring to attributes by
 * name.
 *
 *
 * @author Sabeeh, fleeksoft@gmail.com
 */
public class Attributes : Iterable<Attribute>, Cloneable<Attributes> {
    // the number of instance fields is kept as low as possible giving an object size of 24 bytes
    private var size = 0 // number of slots used (not total capacity, which is keys.length)
    internal var keys: Array<String?> = arrayOfNulls(InitialCapacity)

    // Genericish: all non-internal attribute values must be Strings and are cast on access.
    internal var vals = arrayOfNulls<Any>(InitialCapacity)

    // check there's room for more
    private fun checkCapacity(minNewSize: Int) {
        Validate.isTrue(minNewSize >= size)
        val curCap = keys.size
        if (curCap >= minNewSize) return
        var newCap = if (curCap >= InitialCapacity) size * GrowthFactor else InitialCapacity
        if (minNewSize > newCap) newCap = minNewSize
        keys = keys.copyOf(newCap)
        vals = vals.copyOf(newCap)
    }

    public fun indexOfKey(key: String): Int {
        for (i in 0 until size) {
            if (key == keys[i]) return i
        }
        return NotFound
    }

    private fun indexOfKeyIgnoreCase(key: String): Int {
        for (i in 0 until size) {
            if (key.equals(keys[i], ignoreCase = true)) return i
        }
        return NotFound
    }

    /**
     * Get an attribute value by key.
     * @param key the (case-sensitive) attribute key
     * @return the attribute value if set; or empty string if not set (or a boolean attribute).
     * @see .hasKey
     */
    public operator fun get(key: String): String {
        val i = indexOfKey(key)
        return if (i == NotFound) {
            EmptyString
        } else {
            checkNotNull(
                vals[i],
            )
        }
    }

    /**
     * Get an attribute's value by case-insensitive key
     * @param key the attribute name
     * @return the first matching attribute value if set; or empty string if not set (ora boolean attribute).
     */
    public fun getIgnoreCase(key: String): String {
        val i = indexOfKeyIgnoreCase(key)
        return if (i == NotFound) {
            EmptyString
        } else {
            checkNotNull(
                vals[i],
            )
        }
    }

    /**
     * Get an arbitrary user data object by key.
     * @param key case sensitive key to the object.
     * @return the object associated to this key, or `null` if not found.
     */
//    @Nullable
    public fun getUserData(key: String): Any? {
        var key = key
        if (!isInternalKey(key)) key = internalKey(key)
        val i = indexOfKeyIgnoreCase(key)
        return if (i == NotFound) null else vals[i]
    }

    /**
     * Adds a new attribute. Will produce duplicates if the key already exists.
     * @see Attributes.put
     */
    public fun add(key: String, /*@Nullable*/ value: String?): Attributes {
        addObject(key, value)
        return this
    }

    private fun addObject(key: String, /*@Nullable*/ value: Any?) {
        checkCapacity(size + 1)
        keys[size] = key
        vals[size] = value
        size++
    }

    /**
     * Set a new attribute, or replace an existing one by key.
     * @param key case sensitive attribute key (not null)
     * @param value attribute value (may be null, to set a boolean attribute)
     * @return these attributes, for chaining
     */
    public fun put(key: String, /*@Nullable*/ value: String?): Attributes {
        val i = indexOfKey(key)
        if (i != NotFound) vals[i] = value else add(key, value)
        return this
    }

    /**
     * Put an arbitrary user-data object by key. Will be treated as an internal attribute, so will not be emitted in HTML.
     * @param key case sensitive key
     * @param value object value
     * @return these attributes
     * @see .getUserData
     */
    public fun putUserData(key: String, value: Any): Attributes {
        var key = key
        if (!isInternalKey(key)) key = internalKey(key)
        val i = indexOfKey(key)
        if (i != NotFound) vals[i] = value else addObject(key, value)
        return this
    }

    public fun putIgnoreCase(key: String, /*@Nullable*/ value: String?) {
        val i = indexOfKeyIgnoreCase(key)
        if (i != NotFound) {
            vals[i] = value
            if (keys[i] != key) {
                // case changed, update
                keys[i] = key
            }
        } else {
            add(key, value)
        }
    }

    /**
     * Set a new boolean attribute, remove attribute if value is false.
     * @param key case **insensitive** attribute key
     * @param value attribute value
     * @return these attributes, for chaining
     */
    public fun put(key: String, value: Boolean): Attributes {
        if (value) putIgnoreCase(key, null) else remove(key)
        return this
    }

    /**
     * Set a new attribute, or replace an existing one by key.
     * @param attribute attribute with case sensitive key
     * @return these attributes, for chaining
     */
    public fun put(attribute: Attribute): Attributes {
        put(attribute.key, attribute.value)
        attribute.parent = this
        return this
    }

    // removes and shifts up
    private fun remove(index: Int) {
        Validate.isFalse(index >= size)
        val shifted = size - index - 1
        if (shifted > 0) {
            keys.copyInto(
                destination = keys,
                destinationOffset = index,
                startIndex = index + 1,
                endIndex = index + 1 + shifted,
            )
            vals.copyInto(
                destination = vals,
                destinationOffset = index,
                startIndex = index + 1,
                endIndex = index + 1 + shifted,
            )
        }
        size--
        keys[size] = null // release hold
        vals[size] = null
    }

    /**
     * Remove an attribute by key. **Case sensitive.**
     * @param key attribute key to remove
     */
    public fun remove(key: String) {
        val i = indexOfKey(key)
        if (i != NotFound) remove(i)
    }

    /**
     * Remove an attribute by key. **Case insensitive.**
     * @param key attribute key to remove
     */
    public fun removeIgnoreCase(key: String) {
        val i = indexOfKeyIgnoreCase(key)
        if (i != NotFound) remove(i)
    }

    /**
     * Tests if these attributes contain an attribute with this key.
     * @param key case-sensitive key to check for
     * @return true if key exists, false otherwise
     */
    public fun hasKey(key: String): Boolean {
        return indexOfKey(key) != NotFound
    }

    /**
     * Tests if these attributes contain an attribute with this key.
     * @param key key to check for
     * @return true if key exists, false otherwise
     */
    public fun hasKeyIgnoreCase(key: String): Boolean {
        return indexOfKeyIgnoreCase(key) != NotFound
    }

    /**
     * Check if these attributes contain an attribute with a value for this key.
     * @param key key to check for
     * @return true if key exists, and it has a value
     */
    public fun hasDeclaredValueForKey(key: String): Boolean {
        val i = indexOfKey(key)
        return i != NotFound && vals[i] != null
    }

    /**
     * Check if these attributes contain an attribute with a value for this key.
     * @param key case-insensitive key to check for
     * @return true if key exists, and it has a value
     */
    public fun hasDeclaredValueForKeyIgnoreCase(key: String): Boolean {
        val i = indexOfKeyIgnoreCase(key)
        return i != NotFound && vals[i] != null
    }

    /**
     * Get the number of attributes in this set, including any com.fleeksoft.ksoup internal-only attributes. Internal attributes are
     * excluded from the [.html], [.asList], and [.iterator] methods.
     * @return size
     */
    public fun size(): Int {
        return size
    }

    public fun isEmpty(): Boolean = size == 0

    /**
     * Add all the attributes from the incoming set to this set.
     * @param incoming attributes to add to these attributes.
     */
    public fun addAll(incoming: Attributes) {
        if (incoming.size() == 0) return
        checkCapacity(size + incoming.size)
        val needsPut =
            size != 0 // if this set is empty, no need to check existing set, so can add() vs put()
        // (and save bashing on the indexOfKey()
        for (attr in incoming) {
            if (needsPut) put(attr) else add(attr.key, attr.value)
        }
    }

    override fun iterator(): MutableIterator<Attribute> {
        return object : MutableIterator<Attribute> {
            var expectedSize = size
            var i = 0
            override fun hasNext(): Boolean {
                checkModified()
                while (i < size) {
                    if (isInternalKey(keys[i])) {
                        // skip over internal keys
                        i++
                    } else {
                        break
                    }
                }
                return i < size
            }

            override fun next(): Attribute {
                checkModified()
                val attr = Attribute(keys[i]!!, vals[i] as String?, this@Attributes)
                i++
                return attr
            }

            private fun checkModified() {
                if (size != expectedSize) throw ConcurrentModificationException("Use Iterator#remove() instead to remove attributes while iterating.")
            }

            override fun remove() {
                this@Attributes.remove(--i) // next() advanced, so rewind
                expectedSize--
            }
        }
    }

    /**
     * Get the attributes as a List, for iteration.
     * @return a view of the attributes as an unmodifiable List.
     */
    public fun asList(): List<Attribute> {
        val list: ArrayList<Attribute> = ArrayList(size)
        for (i in 0 until size) {
            if (isInternalKey(keys[i])) continue // skip internal keys
            val attr = Attribute(keys[i]!!, vals[i] as String?, this@Attributes)
            list.add(attr)
        }
        return Collections.unmodifiableList(list)
    }

    /**
     * Retrieves a filtered view of attributes that are HTML5 custom data attributes; that is, attributes with keys
     * starting with `data-`.
     * @return map of custom data attributes.
     */
    public fun dataset(): Dataset {
        return Dataset(this)
    }

    /**
     * Get the HTML representation of these attributes.
     * @return HTML
     */
    public fun html(): String {
        val sb: StringBuilder = StringUtil.borrowBuilder()
        try {
            html(
                sb,
                Document("").outputSettings(),
            ) // output settings a bit funky, but this html() seldom used
        } catch (e: IOException) { // ought never happen
            throw SerializationException(e)
        }
        return StringUtil.releaseBuilder(sb)
    }

    @Throws(IOException::class)
    public fun html(accum: Appendable, out: Document.OutputSettings) {
        val sz = size
        for (i in 0 until sz) {
            if (isInternalKey(keys[i])) continue
            val key: String? = keys[i]?.let { Attribute.getValidKey(it, out.syntax()) }
            if (key != null) {
                Attribute.htmlNoValidate(
                    key,
                    vals[i] as String?,
                    accum.append(' '),
                    out,
                )
            }
        }
    }

    override fun toString(): String {
        return html()
    }

    /**
     * Checks if these attributes are equal to another set of attributes, by comparing the two sets. Note that the order
     * of the attributes does not impact this equality (as per the Map interface equals()).
     * @param o attributes to compare with
     * @return if both sets of attributes have the same content
     */
    override fun equals(/*@Nullable*/ o: Any?): Boolean {
        if (this === o) return true
        if (o == null || this::class != o::class) return false
        val that = o as Attributes
        if (size != that.size) return false
        for (i in 0 until size) {
            val key = keys[i]!!
            val thatI = that.indexOfKey(key)
            if (thatI == NotFound) return false
            val value = vals[i]
            val thatVal = that.vals[thatI]
            if (value == null) {
                if (thatVal != null) return false
            } else if (value != thatVal) return false
        }
        return true
    }

    /**
     * Calculates the hashcode of these attributes, by iterating all attributes and summing their hashcodes.
     * @return calculated hashcode
     */
    override fun hashCode(): Int {
        var result = size
        result = 31 * result + keys.hashCode()
        result = 31 * result + vals.hashCode()
        return result
    }

    override fun clone(): Attributes {
        val attributes = Attributes()
        attributes.addAll(this)

        attributes.size = size

        attributes.keys = keys.copyOf(size)
        attributes.vals = vals.copyOf(size)
        return attributes
    }

    /**
     * Internal method. Lowercases all keys.
     */
    public fun normalize() {
        for (i in 0 until size) {
            keys[i] = lowerCase(keys[i])
        }
    }

    /**
     * Internal method. Removes duplicate attribute by name. Settings for case sensitivity of key names.
     * @param settings case sensitivity
     * @return number of removed dupes
     */
    public fun deduplicate(settings: ParseSettings): Int {
        if (isEmpty()) return 0
        val preserve: Boolean = settings.preserveAttributeCase()
        var dupes = 0
        OUTER@ for (i in keys.indices) {
            var j = i + 1
            while (j < keys.size) {
                if (keys[j] == null) continue@OUTER // keys.length doesn't shrink when removing, so re-test
                if (preserve && keys[i] == keys[j] || !preserve && keys[i].equals(
                        keys[j],
                        ignoreCase = true,
                    )
                ) {
                    dupes++
                    remove(j)
                    j--
                }
                j++
            }
        }
        return dupes
    }

    public class Dataset(private val attributes: Attributes) {
        public val size: Int
            get() = attributes.count { it.isDataAttribute() }

        public operator fun set(key: String, value: String): String? {
            val dataKey = dataKey(key)
            val oldValue = if (attributes.hasKey(dataKey)) attributes[dataKey] else null
            attributes.put(dataKey, value)
            return oldValue
        }

        public operator fun get(key: String): String? {
            val dataKey = "$dataPrefix$key"
            return if (dataKey.length > dataPrefix.length && attributes.hasKey(dataKey)) {
                attributes[dataKey]
            } else {
                null
            }
        }

        public fun remove(key: String) {
            attributes.remove("$dataPrefix$key")
        }
    }

    private fun isInternalKey(key: String?): Boolean {
        return key != null && key.length > 1 && key[0] == InternalPrefix
    }

    internal companion object {
        // The Attributes object is only created on the first use of an attribute; the Element will just have a null
        // Attribute slot otherwise
        const val dataPrefix = "data-"

        // Indicates a com.fleeksoft.ksoup internal key. Can't be set via HTML. (It could be set via accessor, but not too worried about
        // that. Suppressed from list, iter.
        const val InternalPrefix = '/'
        private const val InitialCapacity =
            3 // sampling found mean count when attrs present = 1.49; 1.08 overall. 2.6:1 don't have any attrs.

        // manages the key/val arrays
        private const val GrowthFactor = 2
        const val NotFound = -1
        private const val EmptyString = ""

        // we track boolean attributes as null in values - they're just keys. so returns empty for consumers
        // casts to String, so only for non-internal attributes
        fun checkNotNull(/*@Nullable*/ value: Any?): String {
            return if (value == null) EmptyString else (value as String?)!!
        }

        private fun dataKey(key: String): String {
            return dataPrefix + key
        }

        fun internalKey(key: String): String {
            return InternalPrefix.toString() + key
        }
    }
}
