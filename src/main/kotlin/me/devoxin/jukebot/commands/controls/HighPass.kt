package me.devoxin.jukebot.commands.controls

import me.devoxin.jukebot.audio.filters.HighPassFilter
import me.devoxin.jukebot.framework.*

@CommandProperties(aliases = ["hp"], description = "High pass filter.", category = CommandCategory.CONTROLS)
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class HighPass : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val handler = context.audioPlayer

        val hz = context.args.next("cutoff", ArgumentResolver.INTEGER)
            ?: return context.embed("High Pass", "You need to specify a cut-off frequency ranging from 250 to 20000Hz.")

        if (hz == 0) {
            handler.player.setFilterFactory(null)
            return context.embed("High Pass", "Disabled.")
        }

        if (hz < 250 || hz > 20000) {
            return context.embed("High Pass", "You need to specify a valid number from 250-20000.")
        }

        handler.player.setFilterFactory { _, format, output ->
            listOf(HighPassFilter(output, format.sampleRate, format.channelCount, hz))
        }
        context.embed("High Pass", "Set cut-off frequency to `$hz Hz`.")
    }
}
