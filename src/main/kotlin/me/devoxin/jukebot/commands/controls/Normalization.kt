package me.devoxin.jukebot.commands.controls

import me.devoxin.jukebot.framework.*
import me.devoxin.lavadspx.NormalizationFilter
import net.dv8tion.jda.api.interactions.commands.OptionType

@CommandProperties(aliases = ["norm"], description = "Attenuates peaks above a threshold.", category = CommandCategory.CONTROLS, slashCompatible = true)
@Option(name = "max_amplitude", description = "A number between 0.0-1.0. Higher values are louder.", type = OptionType.INTEGER)
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class Normalization : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val handler = context.audioPlayer

        val maxAmplitude = context.args.next("max_amplitude", ArgumentResolver.FLOAT)
            ?.takeIf { it in 0.0f..1.0f }
            ?: return context.embed("Normalization", "You need to specify maximum amplitude, between 0.0 and 1.0.")

        if (maxAmplitude == 0.0f) {
            handler.player.setFilterFactory(null)
            return context.embed("Normalization", "Disabled.")
        }

        handler.player.setFilterFactory { _, _, output ->
            listOf(NormalizationFilter(output, maxAmplitude))
        }

        context.embed("Normalization", "Set max amplitude to `$maxAmplitude`.")
    }
}
