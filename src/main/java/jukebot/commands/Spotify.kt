package jukebot.commands

import jukebot.JukeBot
import jukebot.audio.SongResultHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context

@CommandProperties(description = "Loads a playlist from Spotify", category = CommandProperties.category.CONTROLS)
public class Spotify : Command(ExecutionType.TRIGGER_CONNECT) { // TODO: Consider moving this to `play` eventually

    override fun execute(context: Context) {
        if (context.donorTier < 2 && !JukeBot.isSelfHosted) {
            return context.embed("Spotify Unavailable", "You must be a [donor in Tier 2 or higher](https://patreon.com/Devoxin)")
        }

        val url = context.getArg(0).replace("<", "").replace(">", "")

        if (url.isEmpty()) {
            return context.embed("Spotify", "You need to specify a URL to a playlist")
        }

        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            player.setChannel(context.channel.idLong)
        }

        JukeBot.playerManager.loadItem("spotify:$url", SongResultHandler(context, player, false))
    }

}
