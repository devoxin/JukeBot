package jukebot.utils

import jukebot.Database
import jukebot.JukeBot
import jukebot.audioutilities.AudioHandler
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

class Context constructor(val event: GuildMessageReceivedEvent, val argString: String, val prefix: String) {

    private val permissions = Permissions()

    val args: Array<String> = argString.split("\\s+".toRegex()).toTypedArray()

    val message: Message = event.message

    val member: Member = event.member

    val author: User = event.author

    val channel: TextChannel = event.channel

    val guild: Guild = event.guild

    val jda: JDA = event.jda

    val donorTier: Int = permissions.getTier(author.idLong)

    fun getArg(index: Int): String {
        return args.getOrNull(index) ?: ""
    }

    fun getAudioPlayer(): AudioHandler {
        return JukeBot.getPlayer(event.guild.audioManager)
    }

    fun ensureVoice(): Boolean {
        val audioManager = guild.audioManager
        val isConnected = audioManager.connectedChannel != null
        val memberVoice = member.voiceState

        if (memberVoice.channel == null) {
            return if (isConnected) {
                sendEmbed("No Mutual VoiceChannel", "You need to join my VoiceChannel!")
                false
            } else {
                sendEmbed("No VoiceChannel", "You need to join a VoiceChannel!")
                false
            }
        }

        if (!isConnected) {
            val connectionError: ConnectionError? = Permissions.canConnectTo(memberVoice.channel)

            if (connectionError != null) {
                sendEmbed(connectionError.title, connectionError.description)
                return false
            }

            audioManager.openAudioConnection(memberVoice.channel)
            return true
        } else if (memberVoice.channel.idLong != audioManager.connectedChannel.idLong) {
            sendEmbed("No Mutual VoiceChannel", "You need to join my VoiceChannel!")
            return false
        } else {
            return true
        }
    }

    fun isDJ(allowLoneVC: Boolean): Boolean {
        val customDjRole: Long? = Database.getDjRole(guild.idLong)
        val roleMatch: Boolean = if (customDjRole != null) {
            member.roles.any { it.idLong == customDjRole }
        } else {
            member.roles.any { it.name.equals("dj", true) }
        }

        val isElevated: Boolean = member.isOwner || JukeBot.botOwnerId == author.idLong || roleMatch

        if (allowLoneVC && !isElevated) {
            return member.voiceState.channel != null && member.voiceState.channel.members.filter { !it.user.isBot }.size == 1
        }

        return isElevated
    }

    fun sendEmbed(title: String, description: String) {
        sendEmbed(title, description, emptyArray(), null)
    }

    fun sendEmbed(title: String?, description: String?, fields: Array<MessageEmbed.Field>) {
        sendEmbed(title, description, fields, null)
    }

    fun sendEmbed(title: String, description: String, footer: String) {
        sendEmbed(title, description, emptyArray(), footer)
    }

    fun sendEmbed(title: String?, description: String?, fields: Array<MessageEmbed.Field>, footer: String?) {
        if (!permissions.canSendTo(channel)) {
            return
        }

        val builder: EmbedBuilder = EmbedBuilder()
                .setColor(JukeBot.embedColour)
                .setTitle(title)
                .setDescription(description)
                .setFooter(footer, null)

        for (field in fields) {
            builder.addField(field)
        }

        event.channel.sendMessage(builder.build()).queue(null) {
            JukeBot.LOG.error("Failed to send message from context!\n" +
                    "\tMessage: ${event.message.contentRaw}\n" +
                    "\tStack: ${it.stackTrace.joinToString("\n")}")
        }
    }

}