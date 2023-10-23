package me.devoxin.jukebot.commands.controls

import me.devoxin.jukebot.audio.filters.LowPassFilter
import me.devoxin.jukebot.framework.*
import net.dv8tion.jda.api.interactions.commands.OptionType

@CommandProperties(aliases = ["lp"], description = "Filter out frequencies above a threshold.", category = CommandCategory.CONTROLS, slashCompatible = true)
@Option(name = "cutoff", description = "The cut-off frequency in Hz, between 0-20000.", type = OptionType.INTEGER)
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class LowPass : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val handler = context.audioPlayer

        val hz = context.args.next("cutoff", ArgumentResolver.INTEGER)
            ?.takeIf { it in 0..20000 }
            ?: return context.embed("Low Pass", "You need to specify a cut-off frequency ranging from 0 to 20000Hz.")

        if (hz == 0) {
            handler.player.setFilterFactory(null)
            return context.embed("Low Pass", "Disabled.")
        }

        handler.player.setFilterFactory { _, format, output ->
            listOf(LowPassFilter(output, format.sampleRate, format.channelCount, hz))
        }

        context.embed("Low Pass", "Set cut-off frequency to `$hz Hz`.")
    }
}
