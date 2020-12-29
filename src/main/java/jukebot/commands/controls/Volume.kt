package jukebot.commands.controls

import jukebot.framework.*
import jukebot.utils.Helpers
import kotlin.math.min

@CommandProperties(aliases = ["vol", "v"], description = "Adjust the player volume", category = CommandCategory.CONTROLS)
@CommandChecks.Dj(alone = false)
@CommandChecks.Playing
class Volume : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (context.args.isEmpty()) {
            val vol = player.player.volume
            return context.embed("Player Volume", "${Helpers.createBar(vol, 250, 10)} `$vol`")
        }

        player.player.volume = min(context.args.first().toIntOrNull() ?: 100, 250)

        val vol = player.player.volume
        context.embed("Player Volume", "${Helpers.createBar(vol, 250, 10)} `$vol`")
    }
}
