package jukebot.commands

import jukebot.JukeBot
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.Helpers

@CommandProperties(description = "Displays the currently playing track", aliases = ["n", "np"], category = CommandProperties.category.MEDIA)
class Now : Command {

    override fun execute(context: Context) {

        val player = JukeBot.getPlayer(context.guild.audioManager)
        val current = player.player.playingTrack


        if (!player.isPlaying) {
            context.sendEmbed("Not Playing", "Nothing is currently playing.")
            return
        }

        val playbackSettings: String = "Shuffle: " + (if (player.isShuffleEnabled) "On" else "Off") + " | Repeat: Off"
        val duration: String = if (current.info.isStream) "LIVE" else Helpers.fTime(current.duration)

        context.sendEmbed("Now Playing",
                "**[${current.info.title}](${current.info.uri})**\n"
                        + "(${Helpers.fTime(current.position)}/$duration)",
                playbackSettings)

    }
}
