package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.nodes.Range.AttributeRange.Companion.UntrackedAttr
import com.fleeksoft.ksoup.parser.ParseSettings
import com.fleeksoft.ksoup.ported.KCloneable
import com.fleeksoft.ksoup.ported.exception.IOException
import com.fleeksoft.ksoup.ported.exception.SerializationException

/**
 * The attributes of an Element.
 * <p>
 * During parsing, attributes in with the same name in an element are deduplicated, according to the configured parser's
 * attribute case-sensitive setting. It is possible to have duplicate attributes subsequently if
 * {@link #add(String, String)} vs {@link #put(String, String)} is used.
 * </p>
 * <p>
 * Attribute name and value comparisons are generally <b>case sensitive</b>. By default for HTML, attribute names are
 * normalized to lower-case on parsing. That means you should use lower-case strings when referring to attributes by
 * name.
 * </p>
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
public class Attributes : Iterable<Attribute>, KCloneable<Attributes> {
    // the number of instance fields is kept as low as possible giving an object size of 24 bytes
    private var size = 0 // number of slots used (not total capacity, which is keys.length)
    internal var keys: Array<String?> = arrayOfNulls(InitialCapacity) // keys is not null, but contents may be. Same for vals
    internal var vals = arrayOfNulls<Any>(InitialCapacity) // Genericish: all non-internal attribute values must be Strings and are cast on access.
    // todo - make keys iterable without creating Attribute objects

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
     * Get an Attribute by key. The Attribute will remain connected to these Attributes, so changes made via
     * [Attribute.setKey], [Attribute.setValue] etc will cascade back to these Attributes and
     * their owning Element.
     * @param key the (case-sensitive) attribute key
     * @return the Attribute for this key, or null if not present.
     */
    public fun attribute(key: String?): Attribute? {
        val i = indexOfKey(key!!)
        return if (i == NotFound) {
            null
        } else {
            Attribute(key, checkNotNull(vals[i]), this)
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
     * Adds a new attribute. Will produce duplicates if the key already exists.
     * @see Attributes.put
     */
    public fun add(
        key: String,
        value: String?,
    ): Attributes {
        addObject(key, value)
        return this
    }

    private fun addObject(
        key: String,
        value: Any?,
    ) {
        checkCapacity(size + 1)
        keys[size] = key
        vals[size] = value
        size++
    }

    /**
     * Set a new attribute, or replace an existing one by key.
     * @param key case sensitive attribute key (not null)
     * @param value attribute value (which can be null, to set a true boolean attribute)
     * @return these attributes, for chaining
     */
    public fun put(
        key: String,
        value: String?,
    ): Attributes {
        val i = indexOfKey(key)
        if (i != NotFound) vals[i] = value else add(key, value)
        return this
    }

    /**
     * Get the map holding any user-data associated with these Attributes. Will be created empty on first use. Held as
     * an internal attribute, not a field member, to reduce the memory footprint of Attributes when not used. Can hold
     * arbitrary objects; use for source ranges, connecting W3C nodes to Elements, etc.
     * @return the map holding user-data
     */
    public fun userData(): MutableMap<String, Any> {
        val userData: MutableMap<String, Any>
        val i = indexOfKey(SharedConstants.UserDataKey)
        if (i == NotFound) {
            userData = HashMap()
            addObject(SharedConstants.UserDataKey, userData)
        } else {
            userData = vals[i] as MutableMap<String, Any>
        }
        return userData
    }

    /**
     * Get an arbitrary user-data object by key.
     * @param key case-sensitive key to the object.
     * @return the object associated to this key, or `null` if not found.
     * @see .userData
     */
    public fun userData(key: String): Any? {
        if (!hasKey(SharedConstants.UserDataKey)) return null // no user data exists

        val userData: Map<String, Any> = userData()
        return userData[key]
    }

    /**
     * Set an arbitrary user-data object by key. Will be treated as an internal attribute, so will not be emitted in HTML.
     * @param key case-sensitive key
     * @param value object value
     * @return these attributes
     * @see .userData
     */
    public fun userData(
        key: String,
        value: Any,
    ): Attributes {
        userData()[key] = value
        return this
    }

    public fun putIgnoreCase(
        key: String,
        value: String?,
    ) {
        val i = indexOfKeyIgnoreCase(key)
        if (i != NotFound) {
            vals[i] = value
            val old = keys[i]
            if (old != null && old != key) {
                // case changed, update
                keys[i] = key
            }
        } else {
            add(key, value)
        }
    }

    /**
     * Set a new boolean attribute. Removes the attribute if the value is false.
     * @param key case <b>insensitive</b> attribute key
     * @param value attribute value
     * @return these attributes, for chaining
     */
    public fun put(
        key: String,
        value: Boolean,
    ): Attributes {
        if (value) putIgnoreCase(key, null) else remove(key)
        return this
    }

    /**
     * Set a new attribute, or replace an existing one by key.
     * @param attribute attribute with case-sensitive key
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
        // todo - exclude internal attributes from this count - maintain size, count of internals
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

    /**
     * Get the source ranges (start to end position) in the original input source from which this attribute's **name**
     * and **value** were parsed.
     *
     * Position tracking must be enabled prior to parsing the content.
     * @param key the attribute name
     * @return the ranges for the attribute's name and value, or `untracked` if the attribute does not exist or its range
     * was not tracked.
     * @see com.fleeksoft.ksoup.parser.Parser.setTrackPosition
     * @see Attribute.sourceRange
     * @see Node.sourceRange
     * @see Element.endSourceRange
     */
    public fun sourceRange(key: String): Range.AttributeRange {
        if (!hasKey(key)) return UntrackedAttr
        val ranges: Map<String, Range.AttributeRange> = getRanges() ?: return UntrackedAttr
        return ranges[key] ?: UntrackedAttr
    }

    /** Get the Ranges, if tracking is enabled; null otherwise.  */
    public fun getRanges(): MutableMap<String, Range.AttributeRange>? {
        return userData(SharedConstants.AttrRangeKey) as MutableMap<String, Range.AttributeRange>?
    }

    override fun iterator(): MutableIterator<Attribute> {
        return object : MutableIterator<Attribute> {
            var expectedSize = size
            var i = 0

            override fun hasNext(): Boolean {
                checkModified()
                while (i < size) {
                    val key = keys[i]
                    require(key != null)
                    if (isInternalKey(key)) { // skip over internal keys
                        i++
                    } else {
                        break
                    }
                }
                return i < size
            }

            override fun next(): Attribute {
                checkModified()
                if (i >= size) throw NoSuchElementException()
                val attr = Attribute(keys[i]!!, vals[i] as String?, this@Attributes)
                i++
                return attr
            }

            private fun checkModified() {
                if (size != expectedSize) {
                    throw ConcurrentModificationException(
                        "Use Iterator#remove() instead to remove attributes while iterating.",
                    )
                }
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
            val key = keys[i]!!
            if (isInternalKey(key)) continue // skip internal keys
            val attr = Attribute(key, vals[i] as String?, this@Attributes)
            list.add(attr)
        }
        return list.toList()
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
            html(sb, Document("").outputSettings()) // output settings a bit funky, but this html() seldom used
        } catch (e: IOException) {
            // ought never happen
            throw SerializationException(e)
        }
        return StringUtil.releaseBuilder(sb)
    }

    public fun html(accum: Appendable, out: Document.OutputSettings) {
        val sz = size
        for (i in 0 until sz) {
            val key = keys[i]!!
            if (isInternalKey(key)) continue
            val validated = Attribute.getValidKey(key, out.syntax())
            if (validated != null) {
                Attribute.htmlNoValidate(validated, vals[i] as String?, accum.append(' '), out)
            }
        }
    }

    override fun toString(): String {
        return html()
    }

    /**
     * Checks if these attributes are equal to another set of attributes, by comparing the two sets. Note that the order
     * of the attributes does not impact this equality (as per the Map interface equals()).
     * @param other attributes to compare with
     * @return if both sets of attributes have the same content
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        val that = other as Attributes
        if (size != that.size) return false
        for (i in 0 until size) {
            val key = keys[i]!!
            val thatI = that.indexOfKey(key)
            if (thatI == NotFound || vals[i] != that.vals[thatI]) return false
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
     * Internal method. Lowercases all (non-internal) keys.
     */
    public fun normalize() {
        for (i in 0 until size) {
            val key = keys[i]!!
            if (!isInternalKey(key)) keys[i] = key.lowercase()
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
        for (i in 0 until size) {
            val keyI = keys[i]
            var j = i + 1
            while (j < size) {
                if ((preserve && keyI == keys[j]) || (!preserve && keyI.equals(keys[j], ignoreCase = true))) {
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

        public operator fun set(
            key: String,
            value: String,
        ): String? {
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

    public companion object {
        // Indicates an internal key. Can't be set via HTML. (It could be set via accessor, but not too worried about
        // that. Suppressed from list, iter.)
        private const val InternalPrefix: Char = '/'

        // The Attributes object is only created on the first use of an attribute; the Element will just have a null
        // Attribute slot otherwise
        public const val dataPrefix: String = "data-"

        // sampling found mean count when attrs present = 1.49; 1.08 overall. 2.6:1 don't have any attrs.
        private const val InitialCapacity = 3

        // manages the key/val arrays
        private const val GrowthFactor = 2

        internal const val NotFound: Int = -1
        private const val EmptyString = ""

        // we track boolean attributes as null in values - they're just keys. so returns empty for consumers
        // casts to String, so only for non-internal attributes
        public fun checkNotNull(value: Any?): String {
            return if (value == null) EmptyString else (value as String)
        }

        private fun dataKey(key: String): String {
            return dataPrefix + key
        }

        public fun internalKey(key: String): String {
            return "$InternalPrefix$key"
        }

        public fun isInternalKey(key: String): Boolean {
            return key.length > 1 && key[0] == InternalPrefix
        }
    }
}
