package me.devoxin.jukebot.commands.controls

import me.devoxin.jukebot.audio.AudioHandler
import me.devoxin.jukebot.framework.*

@CommandProperties(description = "Ends the queue and current track", category = CommandCategory.CONTROLS, slashCompatible = true)
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class Stop : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        if (context.isSlash) {
            context.send(ephemeral = true, { setContent("Stopping music...") })
        }

        val player = context.audioPlayer
        player.repeat = AudioHandler.RepeatMode.NONE
        player.queue.clear()
        player.playNext(false)
    }
}
