package me.devoxin.jukebot.commands.controls

import me.devoxin.jukebot.framework.*
import me.devoxin.jukebot.utils.Helpers
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.math.min

@CommandProperties(aliases = ["vol", "v"], description = "Adjust the player volume.", category = CommandCategory.CONTROLS, slashCompatible = true)
@Option(name = "volume", description = "The new volume level. Omit to see current volume.", type = OptionType.INTEGER, required = false)
@CommandChecks.Dj(alone = false)
@CommandChecks.Playing
class Volume : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val player = context.audioPlayer

        if (!context.args.hasNext("volume")) {
            val volume = player.player.volume
            return context.embed("Player Volume", "${Helpers.createBar(volume, 250, 10)} `$volume`")
        }

        val volume = context.args.next("volume", ArgumentResolver.INTEGER)?.takeIf { it in 0..250 }
            ?: return context.embed("Player Volume", "You provided an invalid number. Volume must be between 0-250.")

        player.player.volume = volume
        context.embed("Player Volume", "${Helpers.createBar(volume, 250, 10)} `$volume`")
    }
}
