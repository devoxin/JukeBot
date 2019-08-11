package jukebot.commands

import jukebot.framework.Command
import jukebot.framework.CommandCategory
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(description = "Skip the track without voting", aliases = ["fs"], category = CommandCategory.CONTROLS)
class Forceskip : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            return context.embed("Not Playing", "Nothing is currently playing.")
        }

        if (!context.isDJ(true) && player.player.playingTrack.userData as Long != context.author.idLong) {
            return context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.serux.pro/faq)")
        }

        player.playNext()
    }
}
