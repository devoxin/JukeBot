package jukebot.commands

import jukebot.audio.AudioHandler
import jukebot.framework.Command
import jukebot.framework.CommandCategory
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(description = "Loop the queue, track or nothing", category = CommandCategory.CONTROLS, aliases = ["loop"])
class Repeat : Command(ExecutionType.REQUIRE_MUTUAL) {

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
            "a", "all" -> player.repeat = AudioHandler.RepeatMode.ALL
            "s", "single" -> player.repeat = AudioHandler.RepeatMode.SINGLE
            "n", "none" -> player.repeat = AudioHandler.RepeatMode.NONE
            else -> {
                return context.embed("Player Repeat", "Current mode: ${player.repeat.humanized()}\nAvailable modes: `s`ingle, `a`ll, `n`one")
            }
        }

        context.embed("Player Repeat", "Set repeat to **${player.repeat.humanized()}**")
    }
}
