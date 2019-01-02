package jukebot.commands

import jukebot.audio.AudioHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.Permissions

@CommandProperties(description = "Loop the queue, track or nothing", category = CommandProperties.category.CONTROLS)
class Repeat : Command(ExecutionType.REQUIRE_MUTUAL) {

    private val permissions = Permissions()

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

        val args = context.args

        when (args[0].toLowerCase()) {
            "a", "all" -> player.setRepeat(AudioHandler.repeatMode.ALL)
            "s", "single" -> player.setRepeat(AudioHandler.repeatMode.SINGLE)
            "n", "none" -> player.setRepeat(AudioHandler.repeatMode.NONE)
            else -> {
                return context.embed("Player Repeat", "Current mode: ${player.repeatMode}\nAvailable modes: `s`ingle, `a`ll, `n`one")
            }
        }

        context.embed("Player Repeat", "Set repeat to **${player.repeatMode}**")

    }
}
