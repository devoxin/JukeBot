package jukebot.commands.playback

import jukebot.framework.*

@CommandProperties(aliases = ["prev", "back"], description = "Plays the last-played track", category = CommandCategory.PLAYBACK)
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class Previous : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (player.previous == null) {
            context.embed("Previous", "There is no previous track stored.")
            return
        }

        player.player.playTrack(player.previous!!.makeClone())
    }
}
