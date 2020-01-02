package jukebot.commands.playback

import jukebot.framework.*

@CommandProperties(description = "Loads a playlist from Spotify", category = CommandCategory.PLAYBACK)
@CommandChecks.Donor(tier = 2)
class Spotify : Command(ExecutionType.TRIGGER_CONNECT) { // TODO: Consider moving this to `play` eventually

    override fun execute(context: Context) {
        context.embed("Command Deprecated", "This feature has been integrated into the `\$play` command.")
    }

}
