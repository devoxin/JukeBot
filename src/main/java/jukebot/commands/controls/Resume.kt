package jukebot.commands.controls

import jukebot.framework.*

@CommandProperties(description = "Resumes the player", category = CommandCategory.CONTROLS)
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class Resume : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val player = context.audioPlayer
        player.player.isPaused = false
        context.react("▶")
    }
}
