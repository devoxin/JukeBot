package jukebot.commands

import jukebot.audio.AudioHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.Permissions
import java.text.DecimalFormat

@CommandProperties(aliases = ["bb"], description = "Bass boosts the audio", category = CommandProperties.category.CONTROLS)
class BassBoost : Command(ExecutionType.REQUIRE_MUTUAL) {

    override fun execute(context: Context) {
        val handler = context.getAudioPlayer()
        val args = context.args

        if (!handler.isPlaying) {
            return context.embed("Not Playing", "Nothing is currently playing.")
        }

        if (!context.isDJ(true)) {
            return context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.serux.pro/faq)")
        }

        if (context.argString.isEmpty()) {
            return context.embed("Bass Boost", "Boosting by ${handler.bassBooster.pcString}%")
        }

        val boost = args[0].toFloatOrNull()

        if (boost == null || boost < 0 || boost > 100) {
            return context.embed("Bass Boost", "You need to specify a valid number from 0-100.")
        }

        handler.bassBooster.boost(boost)
        context.embed("Bass Boost", "Boosting by ${handler.bassBooster.pcString}%")
    }
}
