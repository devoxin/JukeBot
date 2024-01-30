package me.devoxin.jukebot.utils.collections

class FixedDeque<T>(private val capacity: Int) : MutableCollection<T> {
    private val deque = ArrayDeque<T>(capacity)

    override val size: Int get() = deque.size

    override fun isEmpty() = deque.isEmpty()

    override fun containsAll(elements: Collection<T>) = deque.containsAll(elements)

    override fun contains(element: T) = deque.contains(element)

    override fun add(element: T): Boolean {
        checkSizeAndRemove()
        return deque.add(element)
    }

    override fun addAll(elements: Collection<T>) = elements.all(::add)

    fun removeFirst() = deque.removeFirst()

    fun removeLast() = deque.removeLast()

    override fun retainAll(elements: Collection<T>) = deque.retainAll(elements)

    override fun removeAll(elements: Collection<T>) = deque.removeAll(elements)

    override fun remove(element: T) = deque.remove(element)

    override fun clear() = deque.clear()

    override fun iterator() = deque.iterator()

    private fun checkSizeAndRemove() {
        if (deque.size + 1 > capacity) {
            deque.removeFirst()
        }
    }
}
