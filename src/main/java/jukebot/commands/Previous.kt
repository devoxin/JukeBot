package jukebot.commands

import jukebot.framework.Command
import jukebot.framework.CommandChecks
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(aliases = ["prev", "back"], description = "Plays the last-played track")
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