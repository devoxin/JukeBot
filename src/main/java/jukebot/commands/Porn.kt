package jukebot.commands

import jukebot.JukeBot
import jukebot.audioutilities.SongResultHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context


@CommandProperties(description = "Searches for a track on PornHub and queues it", nsfw = true, category = CommandProperties.category.CONTROLS)
class Porn : Command {

    override fun execute(context: Context) {

        if (context.argString.isEmpty()) {
            return context.embed("PornHub Search", "Provide a query to search PornHub for")
        }

        if (!context.channel.isNSFW) {
            return context.embed("PornHub Search", "Searches can only be performed from NSFW channels")
        }

        val player = context.getAudioPlayer()
        val voiceConnected = context.ensureVoice()

        if (!voiceConnected) {
            return
        }

        if (!player.isPlaying) {
            player.setChannel(context.channel.idLong)
        }

        JukeBot.playerManager.loadItem("phsearch:${context.argString}", SongResultHandler(context, player, true))

    }
}
