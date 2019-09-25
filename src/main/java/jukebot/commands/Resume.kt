package jukebot.commands

import jukebot.framework.*

@CommandProperties(description = "Resumes the player", category = CommandCategory.CONTROLS)
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class Resume : Command(ExecutionType.REQUIRE_MUTUAL) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()
        player.player.isPaused = false
        context.react("▶")
    }

}
