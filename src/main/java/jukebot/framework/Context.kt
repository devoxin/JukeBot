package jukebot.framework

import jukebot.Database
import jukebot.JukeBot
import jukebot.audio.AudioHandler
import jukebot.utils.Helpers
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import java.util.concurrent.TimeUnit

class Context(val event: GuildMessageReceivedEvent, val args: List<String>, val originalArgs: String, val prefix: String) {

    val argString: String
        get() = args.joinToString(" ")

    val message = event.message
    val member = event.member!!
    val author = event.author
    val channel = event.channel
    val guild = event.guild
    val jda = event.jda
    val donorTier: Int
        get() {
            if (author.idLong == JukeBot.botOwnerId) {
                return Integer.MAX_VALUE
            }

            if (Database.isPremiumServer(guild.idLong)) {
                return 2
            }

            return Database.getTier(author.idLong)
        }
    val embedColor: Int
        get() = Database.getColour(guild.idLong)

    fun getAudioPlayer(): AudioHandler {
        return JukeBot.getPlayer(guild.idLong)
    }

    fun ensureMutualVoiceChannel(): Boolean {
        val manager = member.guild.audioManager

        return (member.voiceState!!.channel != null
            && manager.connectedChannel != null
            && manager.connectedChannel!!.idLong == member.voiceState!!.channel!!.idLong)
    }

    fun isDJ(allowLoneVC: Boolean): Boolean {
        val customDjRole: Long? = Database.getDjRole(guild.idLong)
        val roleMatch = if (customDjRole != null) {
            customDjRole == guild.publicRole.idLong || member.roles.any { it.idLong == customDjRole }
        } else {
            member.roles.any { it.name.equals("dj", true) }
        }

        val isElevated = member.isOwner || JukeBot.botOwnerId == author.idLong || roleMatch

        if (allowLoneVC && !isElevated) {
            return member.voiceState!!.channel != null && member.voiceState!!.channel!!.members.filter { !it.user.isBot }.size == 1
        }

        return isElevated
    }

    fun embed(title: String, description: String) {
        embed {
            setTitle(title)
            setDescription(description)
        }
    }

    fun embed(block: EmbedBuilder.() -> Unit) {
        if (!Helpers.canSendTo(channel)) {
            return
        }

        val embed = EmbedBuilder()
            .setColor(embedColor)
            .apply(block)
            .build()

        event.channel.sendMessage(embed).queue()
    }

    fun prompt(title: String, description: String, cb: (Message, String?) -> Unit) {
        if (!Helpers.canSendTo(channel)) {
            return
        }

        val embed = EmbedBuilder()
            .setColor(embedColor)
            .setTitle(title)
            .setDescription(description)
            .build()

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