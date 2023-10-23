package me.devoxin.jukebot.commands.controls

import me.devoxin.jukebot.audio.filters.ReverbFilter
import me.devoxin.jukebot.framework.*

@CommandProperties(aliases = ["rev"], description = "Low pass filter.", category = CommandCategory.CONTROLS, enabled = false)
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class Reverb : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val handler = context.audioPlayer

        val decay = context.args.next("decay", ArgumentResolver.DOUBLE)
            ?: return context.embed("Reverb", "You need to specify a valid value for decay.")

        if (decay <= 0.0) {
            handler.player.setFilterFactory(null)
            return context.embed("Reverb", "Disabled.")
        }

        handler.player.setFilterFactory { _, format, output ->
            listOf(ReverbFilter(output, decay.toFloat(), format.channelCount))
        }
        context.embed("Reverb", "yeah boss")
    }
}
