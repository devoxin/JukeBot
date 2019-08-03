package jukebot.commands

import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(description = "Resumes the player", category = CommandProperties.category.CONTROLS)
class Resume : Command(ExecutionType.REQUIRE_MUTUAL) {

    override fun execute(context: Context) {

        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            context.embed("Not Playing", "Nothing is currently playing.")
            return
        }

        if (!context.ensureMutualVoiceChannel()) {
            return
        }

        if (!context.isDJ(true)) {
            context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.serux.pro/faq)")
            return
        }

        player.player.isPaused = false

    }

}
