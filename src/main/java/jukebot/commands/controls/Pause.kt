package jukebot.commands.controls

import jukebot.framework.*

@CommandProperties(description = "Pauses the player", aliases = ["ps"], category = CommandCategory.CONTROLS)
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class Pause : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val player = context.audioPlayer
        player.player.isPaused = true
        context.react("⏸")
    }
}
