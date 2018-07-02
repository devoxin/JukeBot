package jukebot.commands

import jukebot.JukeBot
import jukebot.audioutilities.SongResultHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context

@CommandProperties(description = "Loads a playlist from Spotify", category = CommandProperties.category.CONTROLS)
public class Spotify : Command { // I hate having a separate command for this but it's required due to the way it's implemented

    override fun execute(context: Context) {
        if (context.donorTier < 2 && !JukeBot.isSelfHosted) {
            return context.sendEmbed("Spotify Unavailable", "You must be a [donor in Tier 2 or higher](https://patreon.com/Devoxin)")
        }

        if (context.argString.isEmpty()) {
            context.sendEmbed("Spotify", "You need to specify a URL to a playlist")
            return
        }

        val player = context.getAudioPlayer()
        val voiceConnected = context.ensureVoice()

        if (!voiceConnected) {
            return
        }

        if (!player.isPlaying) {
            player.setChannel(context.channel.idLong)
        }

        val url = context.argString.replace("<", "").replace(">", "")
        JukeBot.playerManager.loadItem("spotify:$url", SongResultHandler(context, player, false))
    }

}