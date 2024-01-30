package me.devoxin.jukebot.extensions

fun <T> Iterable<T>.iterate(range: IntRange) = sequence {
    for (i in range.first until range.last) {
        yield(i to this@iterate.elementAt(i))
    }
}
