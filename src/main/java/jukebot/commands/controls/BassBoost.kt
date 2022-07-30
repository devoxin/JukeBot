package jukebot.commands.controls

import jukebot.framework.*
import jukebot.utils.Helpers

@CommandProperties(aliases = ["bb", "bass"], description = "Bass boosts the audio", category = CommandCategory.CONTROLS)
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class BassBoost : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val handler = context.audioPlayer

        val boost = context.args.firstOrNull()?.toFloatOrNull()
            ?: return context.embed(
                "Bass Boost",
                "${createBar(handler.bassBooster.percentage)} `${handler.bassBooster.percentage.toInt()}`"
            )

        if (boost < 0 || boost > 200) {
            return context.embed("Bass Boost", "You need to specify a valid number from 0-200.")
        }

        handler.bassBooster.boost(boost)
        context.embed("Bass Boost", "${createBar(boost)} `${boost.toInt()}`")
    }

    private fun createBar(v: Float) = Helpers.createBar(v.toInt(), 200, 10)
}
