package jukebot.utils

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageEmbed
import okhttp3.Response
import org.json.JSONObject
import java.awt.Color

fun Response.json(): JSONObject? {
    val body = body()

    body.use {
        return if (isSuccessful && body != null) JSONObject(body.string()) else null
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

fun decodeColor(nm: String): Color? {
    return try {
        Color.decode(nm)
    } catch (e: NumberFormatException) {
        null
    }
}

