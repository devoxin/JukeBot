package jukebot.utils

import jukebot.Database
import jukebot.JukeBot
import jukebot.audio.AudioHandler
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

class Context constructor(val event: GuildMessageReceivedEvent, val argString: String, val prefix: String) {

    private val permissions = Permissions()

    val args = argString.split("\\s+".toRegex())

    val message: Message = event.message

    val member: Member = event.member

    val author: User = event.author

    val channel: TextChannel = event.channel

    val guild: Guild = event.guild

    val jda: JDA = event.jda

    val donorTier: Int = permissions.getTier(author.idLong)

    val embedColor: Int = Database.getColour(guild.idLong)

    fun getArg(index: Int): String {
        return args.getOrNull(index) ?: ""
    }

    fun getAudioPlayer(): AudioHandler {
        return JukeBot.getPlayer(event.guild.audioManager)
    }

    fun ensureMutualVoiceChannel(): Boolean {
        val manager = member.guild.audioManager

        return (member.voiceState.channel != null
                && manager.connectedChannel != null
                && manager.connectedChannel.idLong == member.voiceState.channel.idLong)
    }

    fun isDJ(allowLoneVC: Boolean): Boolean {
        val customDjRole: Long? = Database.getDjRole(guild.idLong)
        val roleMatch: Boolean = if (customDjRole != null) {
            customDjRole == guild.publicRole.idLong || member.roles.any { it.idLong == customDjRole }
        } else {
            member.roles.any { it.name.equals("dj", true) }
        }

        val isElevated: Boolean = member.isOwner || JukeBot.botOwnerId == author.idLong || roleMatch

        if (allowLoneVC && !isElevated) {
            return member.voiceState.channel != null && member.voiceState.channel.members.filter { !it.user.isBot }.size == 1
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

        event.channel.sendMessage(embed).queue(null) {
            JukeBot.LOG.error("Failed to send message from context!\n" +
                    "\tMessage: ${event.message.contentRaw}\n" +
                    "\tStack: ${it.stackTrace.joinToString("\n")}")
        }
    }

}