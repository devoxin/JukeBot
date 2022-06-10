package jukebot.commands.playback

import jukebot.JukeBot
import jukebot.framework.Command
import jukebot.framework.CommandCategory
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(
    description = "Finds a track and queues it to be played next",
    aliases = ["pn"],
    category = CommandCategory.PLAYBACK
)
class PlayNext : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        if (context.args.isEmpty()) {
            return context.embed(name, "You need to specify an identifier to lookup.")
        }

        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            return context.embed("Not Playing", "Nothing is currently playing. Use the `play` command to start a song.")
        }

        JukeBot.playerManager.loadIdentifier(
            "ytsearch:${context.argString}",
            context,
            player,
            useSelection = false,
            playNext = true
        )
    }
}
