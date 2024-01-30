package me.devoxin.jukebot.utils.collections

class FixedSet<T>(private val capacity: Int) : MutableSet<T> {
    private val set = HashSet<T>(capacity)

    override val size: Int get() = set.size

    override fun isEmpty() = set.isEmpty()

    override fun containsAll(elements: Collection<T>) = set.containsAll(elements)

    override fun contains(element: T) = set.contains(element)

    override fun add(element: T): Boolean {
        checkSizeAndRemove()
        return set.add(element)
    }

    override fun addAll(elements: Collection<T>) = elements.all(::add)

    override fun retainAll(elements: Collection<T>) = set.retainAll(elements.toSet())

    override fun removeAll(elements: Collection<T>) = set.removeAll(elements.toSet())

    override fun remove(element: T) = set.remove(element)

    override fun iterator() = set.iterator()

    override fun clear() = set.clear()

    private fun checkSizeAndRemove() {
        if (set.size + 1 > capacity) {
            set.drop(1)
        }
    }
}
