package jukebot.commands

import jukebot.JukeBot
import jukebot.framework.Command
import jukebot.framework.CommandCategory
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(description = "Search SoundCloud and queue the top result", aliases = ["sc"], category = CommandCategory.PLAYBACK)
class ScSearch : Command(ExecutionType.TRIGGER_CONNECT) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            player.channelId = context.channel.idLong
        }

        JukeBot.playerManager.loadIdentifier("scsearch:${context.argString}", context, player, false)
    }
}
