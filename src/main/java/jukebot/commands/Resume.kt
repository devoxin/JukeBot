package jukebot.commands

import jukebot.framework.*

@CommandProperties(description = "Resumes the player", category = CommandCategory.CONTROLS)
@CommandCheck(dj = DjCheck.ROLE_OR_ALONE, isPlaying = true)
class Resume : Command(ExecutionType.REQUIRE_MUTUAL) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()
        player.player.isPaused = false
        context.react("▶")
    }

}
