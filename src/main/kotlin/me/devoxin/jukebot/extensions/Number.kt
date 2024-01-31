package me.devoxin.jukebot.extensions

import me.devoxin.jukebot.utils.Constants
import me.devoxin.jukebot.utils.Helpers

fun Number.createProgressBar(max: Number, length: Number, link: String = Constants.WEBSITE): String {
    return Helpers.createBar(this.toInt(), max.toInt(), length.toInt(), link = link)
}

fun Long.toTimeString(): String {
    val seconds = this / 1000 % 60
    val minutes = this / (1000 * 60) % 60
    val hours = this / (1000 * 60 * 60) % 24
    val days = this / (1000 * 60 * 60 * 24)

    return when {
        days > 0 -> String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds)
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
}

fun Number.plural(s: String): String {
    return "$this ${if (this.toInt() == 1) s else "${s}s"}"
}
