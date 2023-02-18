package me.devoxin.jukebot.commands.controls

import me.devoxin.jukebot.audio.AudioHandler
import me.devoxin.jukebot.framework.*

@CommandProperties(description = "Loop the queue, track or nothing", category = CommandCategory.CONTROLS, aliases = ["loop"])
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class Repeat : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val player = context.audioPlayer

        when (context.args.firstOrNull()?.lowercase()) {
            "a", "all" -> player.repeat = AudioHandler.RepeatMode.ALL
            "s", "single" -> player.repeat = AudioHandler.RepeatMode.SINGLE
            "n", "none" -> player.repeat = AudioHandler.RepeatMode.NONE
            else -> {
                return context.embed(
                    "Player Repeat",
                    "Current mode: ${player.repeat.humanized()}\nAvailable modes: `s`ingle, `a`ll, `n`one"
                )
            }
        }

        context.embed("Player Repeat", "Set repeat to **${player.repeat.humanized()}**")
    }
}
