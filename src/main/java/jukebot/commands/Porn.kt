package jukebot.commands

import jukebot.JukeBot
import jukebot.audioutilities.SongResultHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.Permissions


@CommandProperties(description = "Searches for a track on PornHub and queues it", category = CommandProperties.category.CONTROLS)
class Porn : Command {

    internal val permissions = Permissions()

    override fun execute(context: Context) {

        if (context.argString.isEmpty()) {
            return context.sendEmbed("PornHub Search", "Provide a query to search PornHub for")
        }

        if (!context.channel.isNSFW) {
            return context.sendEmbed("PornHub Search", "Searches can only be performed from NSFW channels")
        }

        val manager = context.guild.audioManager
        val player = JukeBot.getPlayer(manager)

        if (!permissions.checkVoiceConnection(context.member)) {
            return context.sendEmbed("No Mutual VoiceChannel", "Join my VoiceChannel to use this command.")
        }

        if (!manager.isAttemptingToConnect && !manager.isConnected) {
            val connectionStatus = permissions.canConnectTo(context.member.voiceState.channel)

            if (null != connectionStatus) {
                return context.sendEmbed(connectionStatus.title, connectionStatus.description)
            }

            manager.openAudioConnection(context.member.voiceState.channel)
            player.setChannel(context.channel.idLong)
        }

        JukeBot.playerManager.loadItem("phsearch:${context.argString}", SongResultHandler(context, player, true))

    }
}
