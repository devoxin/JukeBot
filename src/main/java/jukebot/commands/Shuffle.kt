package jukebot.commands

import jukebot.framework.*

@CommandProperties(description = "Plays the queue in random order", category = CommandCategory.CONTROLS)
@CommandCheck(dj = DjCheck.ROLE_OR_ALONE, isPlaying = true)
class Shuffle : Command(ExecutionType.REQUIRE_MUTUAL) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()
        player.shuffle = !player.shuffle
        context.embed("Player Shuffle", "Shuffle is now **${if (player.shuffle) "enabled" else "disabled"}**")
    }
}
