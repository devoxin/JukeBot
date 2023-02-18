package me.devoxin.jukebot.commands.controls

import me.devoxin.jukebot.audio.AudioHandler
import me.devoxin.jukebot.framework.*

@CommandProperties(description = "Ends the queue and current track", category = CommandCategory.CONTROLS)
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class Stop : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val player = context.audioPlayer
        player.repeat = AudioHandler.RepeatMode.NONE
        player.queue.clear()
        player.playNext(false)
    }
}
