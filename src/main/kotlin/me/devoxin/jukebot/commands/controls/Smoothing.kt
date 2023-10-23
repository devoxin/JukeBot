package me.devoxin.jukebot.commands.controls

import com.github.natanbc.lavadsp.lowpass.LowPassPcmAudioFilter
import me.devoxin.jukebot.framework.*

@CommandProperties(aliases = ["lpd"], description = "Low pass filter.", category = CommandCategory.CONTROLS)
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class Smoothing : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val handler = context.audioPlayer

        val smoothing = context.args.next("smoothing", ArgumentResolver.INTEGER)
            ?: return context.embed("Low Pass DSP", "You need to specify a smoothing value.")

        if (smoothing == 0) {
            handler.player.setFilterFactory(null)
            return context.embed("Low Pass DSP", "Disabled.")
        }

        handler.player.setFilterFactory { _, format, output ->
            listOf(LowPassPcmAudioFilter(output, format.channelCount))
        }
        context.embed("Low Pass DSP", "Set smoothing to `$smoothing`.")
    }
}
