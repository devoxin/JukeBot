package jukebot.commands

import jukebot.audio.AudioHandler
import jukebot.framework.*

@CommandProperties(description = "Ends the queue and current track", category = CommandCategory.CONTROLS)
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class Stop : Command(ExecutionType.REQUIRE_MUTUAL) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()
        player.repeat = AudioHandler.RepeatMode.NONE
        player.queue.clear()
        player.playNext(false)
    }

}
