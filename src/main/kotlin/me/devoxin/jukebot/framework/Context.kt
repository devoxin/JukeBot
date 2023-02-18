package me.devoxin.jukebot.framework

import me.devoxin.jukebot.Database
import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.audio.AudioHandler
import me.devoxin.jukebot.utils.canSendEmbed
import me.devoxin.jukebot.utils.toMessage
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.concurrent.TimeUnit

class Context(
    val event: MessageReceivedEvent,
    val args: List<String>,
    val originalArgs: String,
    val prefix: String
) {
    val argString: String
        get() = args.joinToString(" ")

    val message = event.message
    val member = event.member!!
    val author = event.author
    val channel = event.channel
    val messageChannel = channel.asGuildMessageChannel()
    val guild = event.guild
    val jda = event.jda
    val donorTier: Int
        get() = when {
                author.idLong == JukeBot.botOwnerId -> Integer.MAX_VALUE
                Database.getIsPremiumServer(guild.idLong) -> 2
                else -> Database.getTier(author.idLong)
        }
    val embedColor: Int
        get() = Database.getColour(guild.idLong)

    val audioPlayer: AudioHandler
        get() = JukeBot.getPlayer(guild.idLong)

    fun isDJ(allowLoneVC: Boolean): Boolean {
        val customDjRoleId = Database.getDjRole(guild.idLong)
        val roleMatch = customDjRoleId?.let { id -> id == guild.publicRole.idLong || member.roles.any { it.idLong == id } }
            ?: member.roles.any { it.name.equals("dj", true) }

        val isElevated = member.isOwner || JukeBot.botOwnerId == author.idLong || roleMatch

        if (allowLoneVC && !isElevated) {
            return member.voiceState!!.channel?.members?.count { !it.user.isBot } == 1
        }

        return isElevated
    }

    fun react(emoji: String) {
        if (message.guild.selfMember.hasPermission(message.channel.asGuildMessageChannel(), Permission.MESSAGE_HISTORY)) {
            message.addReaction(Emoji.fromUnicode(emoji)).queue()
        }
    }

    fun embed(title: String, description: String) = embed {
        setColor(embedColor)
        setTitle(title)
        setDescription(description)
    }

    fun embed(block: EmbedBuilder.() -> Unit) {
        if (!messageChannel.canSendEmbed()) {
            return
        }

        val embed = EmbedBuilder()
            .setColor(embedColor)
            .apply(block)
            .build()
            .toMessage()

        event.channel.sendMessage(embed).queue()
    }

    fun prompt(title: String, description: String, cb: (Message, String?) -> Unit) {
        if (!messageChannel.canSendEmbed()) {
            return
        }

        val embed = EmbedBuilder()
            .setColor(embedColor)
            .setTitle(title)
            .setDescription(description)
            .build()
            .toMessage()

        event.channel.sendMessage(embed).queue { m ->
            JukeBot.waiter.waitForSelection(author.idLong, {
                cb(m, it)
            })
        }
    }

    fun prompt(delay: Int, cb: (String?) -> Unit) {
        JukeBot.waiter.waitForSelection(author.idLong, {
            cb(it)
        }, delay, TimeUnit.SECONDS)
    }
}
