package me.devoxin.jukebot.extensions

fun <T : Any> T?.isNullOr(predicate: (T) -> Boolean): Boolean {
    return this == null || predicate(this)
}
