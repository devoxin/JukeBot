package jukebot.commands

import jukebot.JukeBot
import jukebot.utils.*

@CommandProperties(description = "Displays the currently playing track", aliases = ["n", "np"], category = CommandProperties.category.MEDIA)
class Now : Command {

    override fun execute(context: Context) {

        val player = JukeBot.getPlayer(context.guild.audioManager)
        val current = player.player.playingTrack


        if (!player.isPlaying) {
            context.sendEmbed("Not Playing", "Nothing is currently playing.")
            return
        }

        val playbackSettings = "Shuffle: ${if (player.isShuffleEnabled) "On" else "Off"}" +
                " | Repeat: ${player.repeatMode.toTitleCase()}"
        val duration = if (current.info.isStream) "LIVE" else Helpers.fTime(current.duration)

        context.sendEmbed("Now Playing",
                "**[${current.info.title}](${current.info.uri})**\n"
                        + "(${Helpers.fTime(current.position)}/$duration)",
                playbackSettings)

    }
}
