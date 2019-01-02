package jukebot.commands

import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.Helpers

@CommandProperties(aliases = ["vol", "v"], description = "Adjust the player volume", category = CommandProperties.category.CONTROLS)
class Volume : Command(ExecutionType.REQUIRE_MUTUAL) {

    private val brick = "\u25AC"
    private val maxBricks = 10

    override fun execute(context: Context) {

        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            return context.embed("Not Playing", "Nothing is currently playing.")
        }

        if (context.argString.isEmpty()) {
            val vol = player.player.volume
            return context.embed("Player Volume", "${calculateBricks(vol)} `$vol`")
        }

        if (!context.isDJ(false)) {
            return context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.serux.pro/faq)")
        }

        player.player.volume = Math.min(Helpers.parseNumber(context.argString, 100), 200)

        val vol = player.player.volume
        context.embed("Player Volume", "${calculateBricks(vol)} `$vol`")

    }

    private fun calculateBricks(volume: Int): String {
        val percent = volume.toFloat() / 200
        val blocks = Math.floor((maxBricks * percent).toDouble()).toInt()

        val sb = StringBuilder("[")

        for (i in 0 until maxBricks) {
            if (i == blocks) {
                sb.append("](http://jukebot.serux.pro)")
            }

            sb.append(brick)
        }

        if (blocks == maxBricks) {
            sb.append("](http://jukebot.serux.pro)")
        }

        return sb.toString()
    }
}
