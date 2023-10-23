package me.devoxin.jukebot.commands.controls

import me.devoxin.jukebot.framework.*
import me.devoxin.lavadspx.HighPassFilter
import net.dv8tion.jda.api.interactions.commands.OptionType

@CommandProperties(aliases = ["hp"], description = "Filter out frequencies below a threshold.", category = CommandCategory.CONTROLS, slashCompatible = true)
@Option(name = "cutoff", description = "The cut-off frequency in Hz, between 0-20000.", type = OptionType.INTEGER)
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class HighPass : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val handler = context.audioPlayer

        val hz = context.args.next("cutoff", ArgumentResolver.INTEGER)
            ?.takeIf { it in 0..20000 }
            ?: return context.embed("High Pass", "You need to specify a cut-off frequency ranging from 0 to 20000Hz.")

        if (hz == 0) {
            handler.player.setFilterFactory(null)
            return context.embed("High Pass", "Disabled.")
        }

        handler.player.setFilterFactory { _, format, output ->
            listOf(HighPassFilter(output, format.sampleRate, format.channelCount, hz))
        }

        context.embed("High Pass", "Set cut-off frequency to `$hz Hz`.")
    }
}
