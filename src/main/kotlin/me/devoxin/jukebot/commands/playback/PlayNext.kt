package me.devoxin.jukebot.commands.playback

import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.framework.*
import net.dv8tion.jda.api.interactions.commands.OptionType

@CommandProperties(
    description = "Finds a track and queues it to be played next.",
    aliases = ["pn"],
    category = CommandCategory.PLAYBACK,
    slashCompatible = true
)
@Option(name = "query", description = "The query, or URL to search for.", type = OptionType.STRING)
class PlayNext : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val query = context.args.gatherNext("query").takeIf { it.isNotEmpty() }
            ?: return context.embed(name, "You need to specify an identifier to lookup.")

        val player = context.audioPlayer

        if (!player.isPlaying) {
            return context.embed("Not Playing", "Nothing is currently playing. Use the `play` command to start a song.")
        }

        JukeBot.playerManager.loadIdentifier(
            "${JukeBot.getSearchProvider()}:$query",
            context,
            player,
            useSelection = false,
            playNext = true
        )
    }
}
