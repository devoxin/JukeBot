package jukebot.commands

import jukebot.JukeBot
import jukebot.audio.AudioHandler
import jukebot.audio.SongResultHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context

@CommandProperties(description = "Search SoundCloud and queue the top result", aliases = arrayOf("sc"), category = CommandProperties.category.CONTROLS)
class ScSearch : Command(ExecutionType.TRIGGER_CONNECT) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            player.setChannel(context.channel.idLong)
        }

        JukeBot.playerManager.loadItem("scsearch:${context.argString}", SongResultHandler(context, player, false))
    }
}
