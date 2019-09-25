package jukebot.commands

import jukebot.audio.AudioHandler
import jukebot.framework.*

@CommandProperties(description = "Ends the queue and current track", category = CommandCategory.CONTROLS)
@CommandCheck(dj = DjCheck.ROLE_OR_ALONE, isPlaying = true)
class Stop : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (!player.isPlaying) {

            return
        }

        if (!context.isDJ(true)) {

            return
        }

        player.repeat = AudioHandler.RepeatMode.NONE
        player.queue.clear()
        player.playNext(false)
    }
}
