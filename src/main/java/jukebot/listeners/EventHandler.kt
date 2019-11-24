package jukebot.listeners

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import jukebot.Database
import jukebot.JukeBot
import jukebot.utils.Helpers
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.concurrent.TimeUnit

class EventHandler : ListenerAdapter() {

    private var readyFired = false

    override fun onGuildLeave(event: GuildLeaveEvent) {
        JukeBot.removePlayer(event.guild.idLong)
    }

    override fun onReady(e: ReadyEvent) {
        if (!readyFired) {


            readyFired = true
        }
    }

    override fun onGuildVoiceLeave(e: GuildVoiceLeaveEvent) {
        if (!e.member.user.isBot) {
            handleLeave(e.channelLeft)
        }
    }

    fun handleLeave(channel: VoiceChannel) {
        val audioManager = channel.guild.audioManager

        if (!audioManager.isConnected) {
            return
        }

        val connectedChannel = audioManager.connectedChannel ?: return

        if (!JukeBot.hasPlayer(channel.guild.idLong)) {
            return
        }

        val listeners = connectedChannel.members.filter { !it.user.isBot }.size

        if (listeners == 0) {
            if (!Database.isPremiumServer(channel.guild.idLong) || !Database.getIsAutoDcDisabled(channel.guild.idLong)) {
                JukeBot.removePlayer(channel.guild.idLong)
                audioManager.closeAudioConnection()
            }
        }
    }

}