package me.devoxin.jukebot.framework

import me.devoxin.jukebot.Database
import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.audio.AudioHandler
import me.devoxin.jukebot.utils.canSendEmbed
import me.devoxin.jukebot.utils.toMessage
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageEditData
import org.checkerframework.checker.units.qual.m
import java.util.concurrent.TimeUnit

class Context(
    val args: Arguments,
    val prefix: String,
    val jda: JDA,
    val author: User,
    val member: Member,
    val channel: GuildMessageChannelUnion,
    val guild: Guild,
    val message: Message?,
    val slashEvent: SlashCommandInteractionEvent?
) {
    val isSlash = slashEvent != null

    private var deferred = false
    private var replied = false

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

        if (!isElevated && allowLoneVC) {
            return member.voiceState!!.channel?.members?.count { !it.user.isBot } == 1
        }

        return isElevated
    }

    fun think(ephemeral: Boolean = false) {
        when {
            deferred -> return
            slashEvent != null -> slashEvent.deferReply(ephemeral).queue { deferred = true }
            else -> channel.sendTyping().queue()
        }
    }

    fun react(emoji: String) {
        if (slashEvent != null) {
            return send(ephemeral = true, { setContent(emoji) })
        }

        if (guild.selfMember.hasPermission(channel, Permission.MESSAGE_HISTORY)) {
            message?.addReaction(Emoji.fromUnicode(emoji))?.queue()
        }
    }

    fun embed(title: String, description: String, ephemeral: Boolean = false) = embed(ephemeral) {
        setColor(embedColor)
        setTitle(title)
        setDescription(description)
    }

    fun embed(ephemeral: Boolean = false, block: EmbedBuilder.() -> Unit) {
        if (!channel.canSendEmbed() && slashEvent == null) {
            return
        }

        send(ephemeral, {
            embed {
                setColor(embedColor)
                apply(block)
            }
        })
    }

    fun prompt(title: String, description: String, cb: (Message, String?) -> Unit) {
        if (!channel.canSendEmbed() && slashEvent == null) {
            return
        }

        send(ephemeral = false, {
            embed {
                setColor(embedColor)
                setTitle(title)
                setDescription(description)
            }
        }, {
            JukeBot.waiter.waitForSelection(author.idLong, { s -> cb(it, s) })
        })
    }

    fun prompt(delay: Int, cb: (String?) -> Unit) {
        JukeBot.waiter.waitForSelection(author.idLong, { cb(it) }, delay, TimeUnit.SECONDS)
    }

    fun send(ephemeral: Boolean, builder: MessageCreateBuilder.() -> Unit, cb: (Message) -> Unit = {}) {
        val msg = MessageCreateBuilder().apply(builder).build()

        when {
            slashEvent != null -> {
                when {
                    !deferred && !replied -> slashEvent.reply(msg).setEphemeral(ephemeral).queue { replied = true }
                    deferred && !replied -> slashEvent.hook.editOriginal(MessageEditData.fromCreateData(msg)).queue { replied = true }
                    else -> slashEvent.hook.sendMessage(msg).setEphemeral(ephemeral).queue(cb)
                }
            }
            else -> channel.sendMessage(msg).queue(cb)
        }
    }

    companion object {
        fun MessageCreateBuilder.embed(builder: EmbedBuilder.() -> Unit) {
            this.addEmbeds(EmbedBuilder().apply(builder).build())
        }
    }
}
