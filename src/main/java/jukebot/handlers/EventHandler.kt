package jukebot.handlers

import jukebot.Database
import jukebot.JukeBot
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.hooks.EventListener

class EventHandler : EventListener {
    override fun onEvent(event: GenericEvent) {
        when (event) {
            is GuildLeaveEvent -> onGuildLeave(event)
            is GuildVoiceLeaveEvent -> onGuildVoiceLeave(event)
        }
    }

    private fun onGuildLeave(event: GuildLeaveEvent) {
        JukeBot.removePlayer(event.guild.idLong)
    }

    private fun onGuildVoiceLeave(e: GuildVoiceLeaveEvent) {
        when {
            !e.member.user.isBot -> return handleLeave(e.channelLeft)
            e.member == e.guild.selfMember -> return JukeBot.removePlayer(e.guild.idLong)
        }
    }

    private fun handleLeave(channel: VoiceChannel) {
        if (!JukeBot.hasPlayer(channel.guild.idLong)) {
            return
        }

        val audioManager = channel.guild.audioManager

        if (!audioManager.isConnected) {
            return
        }

        val connectedChannel = audioManager.connectedChannel ?: return
        val isAlone = connectedChannel.members.none { !it.user.isBot }

        if (isAlone) {
            if (!Database.getIsPremiumServer(channel.guild.idLong) || !Database.getIsAutoDcDisabled(channel.guild.idLong)) {
                JukeBot.removePlayer(channel.guild.idLong)
                audioManager.closeAudioConnection()
            }
        }
    }
}
