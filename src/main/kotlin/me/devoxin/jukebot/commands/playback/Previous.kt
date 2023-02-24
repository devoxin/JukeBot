package me.devoxin.jukebot.commands.playback

import me.devoxin.jukebot.framework.*

@CommandProperties(
    aliases = ["prev", "back"],
    description = "Plays the last-played track",
    category = CommandCategory.PLAYBACK,
    slashCompatible = true
)
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class Previous : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val player = context.audioPlayer

        if (player.previous == null) {
            return context.embed("Previous", "There is no previous track stored.")
        }

        player.player.playTrack(player.previous!!.makeClone())
        context.react("⏮")
    }
}
