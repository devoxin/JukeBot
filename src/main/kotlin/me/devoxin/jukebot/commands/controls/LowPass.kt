package me.devoxin.jukebot.commands.controls

import me.devoxin.jukebot.audio.filters.LowPassFilter
import me.devoxin.jukebot.framework.*

@CommandProperties(aliases = ["lp"], description = "Low pass filter.", category = CommandCategory.CONTROLS)
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class LowPass : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val handler = context.audioPlayer

        val hz = context.args.next("cutoff", ArgumentResolver.INTEGER)
            ?: return context.embed("Low Pass", "You need to specify a cut-off frequency ranging from 0 to 250Hz.")

        if (hz == 0) {
            handler.player.setFilterFactory(null)
            return context.embed("Low Pass", "Disabled.")
        }

        if (hz <= 0 || hz > 250) {
            return context.embed("Low Pass", "You need to specify a valid number from 0-250Hz.")
        }

        handler.player.setFilterFactory { _, format, output ->
            listOf(LowPassFilter(output, format.sampleRate, format.channelCount, hz))
        }
        context.embed("Low Pass", "Set cut-off frequency to `$hz Hz`.")
    }
}
