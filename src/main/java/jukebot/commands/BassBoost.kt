package jukebot.commands

import jukebot.audio.AudioHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.Permissions

@CommandProperties(aliases = ["bb"], description = "Bass boosts the audio", category = CommandProperties.category.CONTROLS)
class BassBoost : Command(ExecutionType.REQUIRE_MUTUAL) {

    private val permissions = Permissions()

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
            return context.embed("BassBoost Presets",
                    "Current Setting: `" + handler.bassBoostSetting + "`\n\nValid presets: `Off`, `Weak`, `Medium`, `Strong`, `Insane`, `Wtf`")
        }

        when (args[0].toLowerCase()) {
            "o", "off" -> handler.bassBoost(AudioHandler.bassBoost.OFF)
            "w", "weak" -> handler.bassBoost(AudioHandler.bassBoost.WEAK)
            "m", "medium" -> handler.bassBoost(AudioHandler.bassBoost.MEDIUM)
            "s", "strong" -> handler.bassBoost(AudioHandler.bassBoost.STRONG)
            "i", "insane" -> handler.bassBoost(AudioHandler.bassBoost.INSANE)
            "wtf" -> handler.bassBoost(AudioHandler.bassBoost.WTF)
            else -> return context.embed("BassBoost", args[0] + " is not a recognised preset")
        }

        context.embed("BassBoost", "Set bass boost to `${handler.bassBoostSetting}`")
    }
}
