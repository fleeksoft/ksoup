package com.fleeksoft.ksoup.helper

import com.fleeksoft.ksoup.removeRange

/**
 * Implementation of ArrayList that watches out for changes to the contents.
 */
abstract class ChangeNotifyingArrayList<E>(initialCapacity: Int) : MutableList<E> {

    private val delegate: MutableList<E> = ArrayList(initialCapacity)

    abstract fun onContentsChanged()

    override fun set(index: Int, element: E): E {
        onContentsChanged()
        return delegate.set(index, element)
    }

    override fun add(element: E): Boolean {
        onContentsChanged()
        return delegate.add(element)
    }

    override fun add(index: Int, element: E) {
        onContentsChanged()
        delegate.add(index, element)
    }

    override fun removeAt(index: Int): E {
        onContentsChanged()
        return delegate.removeAt(index)
    }

    override fun remove(element: E): Boolean {
        onContentsChanged()
        return delegate.remove(element)
    }

    override fun clear() {
        onContentsChanged()
        delegate.clear()
    }

    override fun addAll(elements: Collection<E>): Boolean {
        onContentsChanged()
        return delegate.addAll(elements)
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        onContentsChanged()
        return delegate.addAll(index, elements)
    }

    fun removeRange(fromIndex: Int, toIndex: Int) {
        onContentsChanged()
        (delegate as ArrayList<E>).removeRange(fromIndex, toIndex)
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        onContentsChanged()
        return delegate.removeAll(elements)
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        onContentsChanged()
        return delegate.retainAll(elements)
    }

    override val size: Int get() = delegate.size
    override fun contains(element: E) = delegate.contains(element)
    override fun containsAll(elements: Collection<E>) = delegate.containsAll(elements)
    override fun get(index: Int): E = delegate[index]
    override fun indexOf(element: E): Int = delegate.indexOf(element)
    override fun isEmpty(): Boolean = delegate.isEmpty()
    override fun iterator(): MutableIterator<E> = delegate.iterator()
    override fun lastIndexOf(element: E): Int = delegate.lastIndexOf(element)
    override fun listIterator(): MutableListIterator<E> = delegate.listIterator()
    override fun listIterator(index: Int): MutableListIterator<E> = delegate.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> =
        delegate.subList(fromIndex, toIndex)
}
