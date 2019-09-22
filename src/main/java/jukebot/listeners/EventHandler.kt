package jukebot.listeners

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import jukebot.JukeBot
import jukebot.utils.Helpers
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.concurrent.TimeUnit

class EventHandler : ListenerAdapter() {

    override fun onGuildLeave(event: GuildLeaveEvent) {
        JukeBot.removePlayer(event.guild.idLong)
    }

    override fun onReady(e: ReadyEvent) {
        if (!JukeBot.isReady) {
            e.jda.retrieveApplicationInfo().queue { info ->
                JukeBot.selfId = info.idLong;
                JukeBot.botOwnerId = info.owner.idLong
                JukeBot.isSelfHosted = info.idLong != 249303797371895820L && info.idLong != 314145804807962634L

                if (JukeBot.isSelfHosted) {
                    CommandHandler.commands.remove("patreon")
                    //CommandHandler.commands.remove("verify")
                    CommandHandler.commands.remove("feedback")?.destroy()
                } else {
                    Helpers.monitor.scheduleAtFixedRate(Helpers::monitorPledges, 0, 1, TimeUnit.DAYS)
                }

                if (info.idLong == 314145804807962634L || JukeBot.isSelfHosted)
                    JukeBot.playerManager.configuration.resamplingQuality = AudioConfiguration.ResamplingQuality.HIGH

                JukeBot.isReady = true
            }
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
            JukeBot.removePlayer(channel.guild.idLong)
            audioManager.closeAudioConnection()
        }
    }

}