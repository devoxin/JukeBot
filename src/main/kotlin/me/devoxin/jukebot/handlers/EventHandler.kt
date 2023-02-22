package me.devoxin.jukebot.handlers

import me.devoxin.jukebot.Database
import me.devoxin.jukebot.JukeBot
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.EventListener

class EventHandler : EventListener {
    override fun onEvent(event: GenericEvent) {
        when (event) {
            is GuildLeaveEvent -> onGuildLeave(event)
            is GuildVoiceUpdateEvent -> {
                if (event.channelJoined == null && event.channelLeft != null) onGuildVoiceLeave(event)
                if (event.channelJoined != null && event.member.idLong == event.jda.selfUser.idLong) (event.channelJoined as? StageChannel)?.requestToSpeak()?.queue()
            }
        }
    }

    private fun onGuildLeave(event: GuildLeaveEvent) {
        JukeBot.removePlayer(event.guild.idLong)
    }

    private fun onGuildVoiceLeave(e: GuildVoiceUpdateEvent) {
        when {
            !e.member.user.isBot -> return handleLeave(e.channelLeft!!)
            e.member.idLong == e.jda.selfUser.idLong -> return JukeBot.removePlayer(e.guild.idLong)
        }
    }

    private fun handleLeave(channel: AudioChannel) {
        if (!JukeBot.hasPlayer(channel.guild.idLong)) {
            return
        }

        val audioManager = channel.guild.audioManager.takeIf { it.isConnected } ?: return
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