package me.devoxin.jukebot.extensions

fun <T> List<T>.separate(): Pair<T, List<T>> = first() to drop(1)
