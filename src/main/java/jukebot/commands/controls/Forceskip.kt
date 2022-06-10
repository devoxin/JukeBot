package jukebot.commands.controls

import jukebot.framework.*
import jukebot.utils.Constants

@CommandProperties(description = "Skip the track without voting", aliases = ["fs"], category = CommandCategory.CONTROLS)
@CommandChecks.Playing
class Forceskip : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (!context.isDJ(true) && player.player.playingTrack.userData as Long != context.author.idLong) {
            return context.embed(
                "Not a DJ",
                "You need to be a DJ to use this command.\n[See here on how to become a DJ](${Constants.WEBSITE}/faq)"
            )
        }

        player.playNext()
    }
}
