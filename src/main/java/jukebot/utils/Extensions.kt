package jukebot.utils

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import okhttp3.Response
import java.awt.Color

fun Response.json(): JsonObject? {
    body().use {
        return if (isSuccessful && it != null) JsonParser.`object`().from(it.string()) else null
    }
}

fun String.toTitleCase(): String {
    return this[0].toUpperCase() + this.substring(1)
}

fun Message.editEmbed(block: EmbedBuilder.() -> Unit) {
    this.editMessage(EmbedBuilder().apply(block).build()).queue()
}

fun EmbedBuilder.addFields(fields: Array<MessageEmbed.Field>) {
    for (field in fields) {
        this.addField(field)
    }
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

fun String.toColorOrNull() = try {
    Color.decode(this)
} catch (e: NumberFormatException) {
    null
}

fun <T> List<T>.separate(): Pair<T, List<T>> = Pair(first(), drop(1))

fun <T> Iterable<T>.iterate(range: IntRange) = sequence {
    for (i in range.first until range.last) {
        yield(Pair(i, this@iterate.elementAt(i)))
    }
}
