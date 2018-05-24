package jukebot.utils

import jukebot.JukeBot
import jukebot.audioutilities.AudioHandler
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

class Context constructor(val event: GuildMessageReceivedEvent, val argString: String, val prefix: String) {

    val args: Array<String> = argString.split("\\s+".toRegex()).toTypedArray()

    val message: Message = event.message

    val member: Member = event.member

    val author: User = event.author

    val channel: TextChannel = event.channel

    val guild: Guild = event.guild

    val jda: JDA = event.jda

    fun getAudioPlayer(): AudioHandler {
        return JukeBot.getPlayer(event.guild.audioManager)
    }

    fun isDJ(allowLoneVC: Boolean): Boolean {
        val isElevated: Boolean = member.isOwner || JukeBot.botOwnerId == author.idLong || member.roles.any { "dj" == it.name.toLowerCase() }

        if (allowLoneVC && !isElevated) {
            return member.voiceState.channel != null && member.voiceState.channel.members.filter { !it.user.isBot }.size == 1
        }

        return isElevated
    }

    fun sendEmbed(title: String, description: String) {
        sendEmbed(title, description, emptyArray(), "")
    }

    fun sendEmbed(title: String, description: String, fields: Array<MessageEmbed.Field>) {
        sendEmbed(title, description, fields, "")
    }

    fun sendEmbed(title: String, description: String, footer: String) {
        sendEmbed(title, description, emptyArray(), footer)
    }

    fun sendEmbed(title: String, description: String, fields: Array<MessageEmbed.Field>, footer: String) {
        val builder: EmbedBuilder = EmbedBuilder()
                .setColor(JukeBot.embedColour)
                .setTitle(title)
                .setDescription(description)
                .setFooter(footer, null)

        for (field in fields) {
            builder.addField(field)
        }

        event.channel.sendMessage(builder.build()).queue()
    }

}