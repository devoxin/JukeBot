package jukebot.commands

import jukebot.JukeBot
import jukebot.audioutilities.SongResultHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Permissions
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent


@CommandProperties(description = "Find a track on YouTube and queue it", aliases = ["p"], category = CommandProperties.category.CONTROLS)
class Porn : Command {

    internal val permissions = Permissions()

    override fun execute(e: GuildMessageReceivedEvent, query: String) {

        if (query.isEmpty()) {
            e.channel.sendMessage(EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Specify something")
                    .setDescription("YouTube: Search Term/URL\nSoundCloud: URL")
                    .build()
            ).queue()
            return
        }

        val manager = e.guild.audioManager
        val player = JukeBot.getPlayer(manager)

        if (!permissions.checkVoiceConnection(e.member)) {
            e.channel.sendMessage(EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("No Mutual VoiceChannel")
                    .setDescription("Join my VoiceChannel to use this command.")
                    .build()
            ).queue()
            return
        }

        if (!manager.isAttemptingToConnect && !manager.isConnected) {
            val connectionStatus = permissions.canConnectTo(e.member.voiceState.channel)

            if (null != connectionStatus) {
                e.channel.sendMessage(EmbedBuilder()
                        .setColor(JukeBot.embedColour)
                        .setTitle(connectionStatus.title)
                        .setDescription(connectionStatus.description)
                        .build()
                ).queue()
                return
            }

            manager.openAudioConnection(e.member.voiceState.channel)
            player.setChannel(e.channel.idLong)
        }

        if (!e.channel.isNSFW) {
            e.channel.sendMessage("Pornhub searches can only be loaded in NSFW channels").queue()

            if (!player.isPlaying)
                e.guild.audioManager.closeAudioConnection()

            return
        }

        JukeBot.playerManager.loadItem("phsearch:$query", SongResultHandler(e, player, true))

    }
}
