package me.devoxin.jukebot.commands.controls

import me.devoxin.jukebot.audio.filters.NormalizationFilter
import me.devoxin.jukebot.framework.*

@CommandProperties(aliases = ["norm"], description = "Normalization filter.", category = CommandCategory.CONTROLS, enabled = true)
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class Normalization : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val handler = context.audioPlayer

        handler.player.setFilterFactory { _, _, output ->
            listOf(NormalizationFilter(output, 0.5f))
        }
        context.embed("Normalization", "...")
    }
}
