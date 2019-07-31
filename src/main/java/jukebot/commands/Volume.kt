package jukebot.commands

import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.Helpers

@CommandProperties(aliases = ["vol", "v"], description = "Adjust the player volume", category = CommandProperties.category.CONTROLS)
class Volume : Command(ExecutionType.REQUIRE_MUTUAL) {


    private val maxBricks = 10

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            return context.embed("Not Playing", "Nothing is currently playing.")
        }

        // If listeners == 1, ask them to use Discord volume slider?

        if (context.argString.isEmpty()) {
            val vol = player.player.volume
            return context.embed("Player Volume", "${Helpers.createBar(vol, 250, 10)} `$vol`")
        }

        if (!context.isDJ(false)) {
            return context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.serux.pro/faq)")
        }

        player.player.volume = Math.min(Helpers.parseNumber(context.argString, 100), 250)

        val vol = player.player.volume
        context.embed("Player Volume", "${Helpers.createBar(vol, 250, 10)} `$vol`")
    }


}
