package jukebot.commands

import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.toTimeString

@CommandProperties(description = "Move to the specified position in the track", category = CommandProperties.category.CONTROLS)
class Seek : Command {

    override fun execute(context: Context) {

        val player = context.getAudioPlayer()
        val currentTrack = player.player.playingTrack

        if (!player.isPlaying) {
            return context.embed("Not Playing", "Nothing is currently playing.")
        }

        if (!context.isDJ(true)) {
            return context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.xyz/faq)")
        }

        if (!currentTrack.isSeekable) {
            return context.embed("Seek Unavailable", "The current track doesn't support seeking.")
        }

        val forwardTime = (context.argString.toIntOrNull() ?: 10) * 1000

        if (currentTrack.position + forwardTime >= currentTrack.duration) {
            return player.playNext()
        }

        currentTrack.position = currentTrack.position + forwardTime
        context.embed("Track Seeking", "The current track has been moved to **${currentTrack.position.toTimeString()}**")

    }
}
