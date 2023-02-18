package me.devoxin.jukebot.utils

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import okhttp3.Response
import java.awt.Color
import java.sql.ResultSet

fun Response.json(): JsonObject? {
    body.use {
        return if (isSuccessful && it != null) JsonParser.`object`().from(it.string()) else null
    }
}

fun String.capitalise(): String {
    return this[0].uppercase() + this.substring(1)
}

fun MessageEmbed.toMessage(): MessageCreateData = MessageCreateData.fromEmbeds(this)

fun EmbedBuilder.addFields(fields: Array<MessageEmbed.Field>) {
    for (field in fields) {
        this.addField(field)
    }
}

fun Message.editEmbed(block: EmbedBuilder.() -> Unit) {
    this.editMessage(MessageEditData.fromEmbeds(EmbedBuilder().apply(block).build())).queue()
}

fun GuildMessageChannel.canSendEmbed() = this.canTalk() && this.guild.selfMember.hasPermission(this, Permission.MESSAGE_EMBED_LINKS)

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

fun <T> List<T>.separate(): Pair<T, List<T>> = first() to drop(1)

fun <T> Iterable<T>.iterate(range: IntRange) = sequence {
    for (i in range.first until range.last) {
        yield(Pair(i, this@iterate.elementAt(i)))
    }
}

fun <T> MutableSet<T>.trimToSize(maxCapacity: Int) {
    if (this.size > maxCapacity) {
        this.drop(this.size - maxCapacity)
    }
}

operator fun ResultSet.get(key: String): String = this.getString(key)
