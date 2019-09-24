package jukebot.commands

import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(aliases = ["prev", "back"], description = "Plays the last-played track")
class Previous : Command(ExecutionType.REQUIRE_MUTUAL) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            context.embed("Not Playing", "Nothing is currently playing.")
            return
        }

        if (!context.isDJ(true)) {
            context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.serux.pro/faq)")
            return
        }

        if (player.previous == null) {
            context.embed("Previous", "There is no previous track stored.")
            return
        }

        player.player.playTrack(player.previous!!.makeClone())
    }

}