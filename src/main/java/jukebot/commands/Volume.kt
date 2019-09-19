package jukebot.commands

import jukebot.framework.Command
import jukebot.framework.CommandCategory
import jukebot.framework.CommandProperties
import jukebot.framework.Context
import jukebot.utils.Helpers
import kotlin.math.min

@CommandProperties(aliases = ["vol", "v"], description = "Adjust the player volume", category = CommandCategory.CONTROLS)
class Volume : Command(ExecutionType.REQUIRE_MUTUAL) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            return context.embed("Not Playing", "Nothing is currently playing.")
        }

        // If listeners == 1, ask them to use Discord volume slider?

        if (context.args.isEmpty()) {
            val vol = player.player.volume
            return context.embed("Player Volume", "${Helpers.createBar(vol, 250, 10)} `$vol`")
        }

        if (!context.isDJ(false)) {
            return context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.serux.pro/faq)")
        }

        player.player.volume = min(context.args.first().toIntOrNull() ?: 100, 250)

        val vol = player.player.volume
        context.embed("Player Volume", "${Helpers.createBar(vol, 250, 10)} `$vol`")
    }


}
