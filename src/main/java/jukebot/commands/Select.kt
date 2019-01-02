package jukebot.commands

import jukebot.JukeBot
import jukebot.audio.AudioHandler
import jukebot.audio.SongResultHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context

@CommandProperties(description = "Search YouTube and select from up to 5 tracks", aliases = arrayOf("search", "sel", "s"), category = CommandProperties.category.CONTROLS)
class Select : Command(ExecutionType.TRIGGER_CONNECT) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            player.setChannel(context.channel.idLong)
        }

        JukeBot.playerManager.loadItem("ytsearch:" + context.argString, SongResultHandler(context, player, true))
    }
}
