package me.devoxin.jukebot.commands.playback

import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.framework.Command
import me.devoxin.jukebot.framework.CommandCategory
import me.devoxin.jukebot.framework.CommandProperties
import me.devoxin.jukebot.framework.Context

@CommandProperties(description = "Finds a track and queues it to be played next", aliases = ["pn"], category = CommandCategory.PLAYBACK)
class PlayNext : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        if (context.args.isEmpty()) {
            return context.embed(name, "You need to specify an identifier to lookup.")
        }

        val player = context.audioPlayer

        if (!player.isPlaying) {
            return context.embed("Not Playing", "Nothing is currently playing. Use the `play` command to start a song.")
        }

        JukeBot.playerManager.loadIdentifier("${JukeBot.getSearchProvider()}:${context.argString}", context, player, useSelection = false, playNext = true)
    }
}
