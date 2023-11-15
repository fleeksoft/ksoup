package com.fleeksoft.ksoup.ported

internal class IdentityHashMap<K, V> : MutableMap<K, V> {
    private val delegate: MutableMap<IdentityWrapper<K>, V> = mutableMapOf<IdentityWrapper<K>, V>()

    private class IdentityWrapper<T>(val value: T) {
        override fun hashCode(): Int {
            return value.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is IdentityWrapper<*> && other.value === value
        }
    }

    private class IdentityEntry<K, V>(
        private val original: MutableMap.MutableEntry<IdentityWrapper<K>, V>,
    ) : MutableMap.MutableEntry<K, V> {
        override val key: K
            get() = original.key.value
        override val value: V
            get() = original.value

        override fun setValue(newValue: V): V {
            return original.setValue(newValue)
        }
    }

    override val size: Int
        get() = delegate.size

    override fun containsKey(key: K): Boolean {
        return delegate.containsKey(IdentityWrapper(key))
    }

    override fun containsValue(value: V): Boolean {
        return delegate.containsValue(value)
    }

    override fun get(key: K): V? {
        return delegate[IdentityWrapper(key)]
    }

    override fun isEmpty(): Boolean {
        return delegate.isEmpty()
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = delegate.entries.map { IdentityEntry(it) }.toMutableSet()

    override val keys: MutableSet<K>
        get() = delegate.keys.map { it.value }.toMutableSet()

    override val values: MutableCollection<V>
        get() = delegate.values

    override fun clear() {
        delegate.clear()
    }

    override fun put(key: K, value: V): V? {
        return delegate.put(IdentityWrapper(key), value)
    }

    override fun putAll(from: Map<out K, V>) {
        from.forEach { (k, v) -> this[k] = v }
    }

    override fun remove(key: K): V? {
        return delegate.remove(IdentityWrapper(key))
    }
}
