package jukebot.commands

import jukebot.framework.*

@CommandProperties(description = "Skip the track without voting", aliases = ["fs"], category = CommandCategory.CONTROLS)
@CommandCheck(isPlaying = true)
class Forceskip : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (!context.isDJ(true) && player.player.playingTrack.userData as Long != context.author.idLong) {
            return context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.serux.pro/faq)")
        }

        player.playNext()
    }
}
