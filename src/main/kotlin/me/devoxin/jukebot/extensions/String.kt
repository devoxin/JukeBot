package me.devoxin.jukebot.extensions

import java.awt.Color

fun String.capitalise(): String {
    return this[0].uppercase() + this.substring(1)
}

fun String.toColorOrNull() = runCatching(Color::decode).getOrNull()

fun String.parseAsTimeStringToMillisecondsOrNull(): Long? {
    val parsed = if (':' in this) {
        val segments = split(":").map { it.toLongOrNull() }

        if (segments.any { it == null }) {
            return null
        }

        val filtered = segments.filterNotNull()

        when (filtered.size) {
            3 -> (filtered[0] * 3600) + (filtered[1] * 60) + filtered[2] // HH, MM, SS
            2 -> (filtered[0] * 60) + filtered[1] // MM, SS
            else -> null
        }
    } else {
        toLongOrNull()
    }

    return parsed?.times(1000)
}
