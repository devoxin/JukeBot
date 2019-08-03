package jukebot.commands

import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context
import jukebot.utils.toTimeString

@CommandProperties(description = "Move to the specified position in the track", category = CommandProperties.category.CONTROLS, aliases = ["jump"])
class Seek : Command(ExecutionType.REQUIRE_MUTUAL) {

    override fun execute(context: Context) {

        val player = context.getAudioPlayer()
        val currentTrack = player.player.playingTrack

        if (!player.isPlaying) {
            return context.embed("Not Playing", "Nothing is currently playing.")
        }

        if (!context.isDJ(true)) {
            return context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.serux.pro/faq)")
        }

        if (!currentTrack.isSeekable) {
            return context.embed("Seek Unavailable", "The current track doesn't support seeking.")
        }

        val jumpTime = context.args.firstOrNull()?.toIntOrNull()
                ?: return context.embed("Track Seeking", "You need to specify a valid amount of seconds to jump.")

        val jumpTimeMs = jumpTime * 1000

        if (currentTrack.position + jumpTimeMs >= currentTrack.duration) {
            return player.playNext()
        }

        currentTrack.position = currentTrack.position + jumpTimeMs
        context.embed("Track Seeking", "The current track has been moved to **${currentTrack.position.toTimeString()}**")

    }
}
