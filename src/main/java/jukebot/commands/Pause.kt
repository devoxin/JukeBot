package jukebot.commands

import jukebot.framework.*

@CommandProperties(description = "Pauses the player", aliases = ["ps"], category = CommandCategory.CONTROLS)
@CommandCheck(dj = DjCheck.ROLE_OR_ALONE, isPlaying = true)
class Pause : Command(ExecutionType.REQUIRE_MUTUAL) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()
        player.player.isPaused = true
        context.react("⏸")
    }

}
