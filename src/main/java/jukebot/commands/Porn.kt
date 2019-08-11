package jukebot.commands

import jukebot.JukeBot
import jukebot.audio.SongResultHandler
import jukebot.framework.Command
import jukebot.framework.CommandCategory
import jukebot.framework.CommandProperties
import jukebot.framework.Context


@CommandProperties(description = "Searches for a track on PornHub and queues it", nsfw = true, category = CommandCategory.PLAYBACK)
class Porn : Command(ExecutionType.TRIGGER_CONNECT) {

    override fun execute(context: Context) {
        if (!context.channel.isNSFW) {
            return context.embed("PornHub Search", "Searches can only be performed from NSFW channels")
        }

        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            player.channelId = context.channel.idLong
        }

        JukeBot.playerManager.loadItem("phsearch:${context.argString}", SongResultHandler(context, player, true))
    }
}
