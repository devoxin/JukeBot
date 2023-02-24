package me.devoxin.jukebot.commands.playback

import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.framework.Command
import me.devoxin.jukebot.framework.CommandCategory
import me.devoxin.jukebot.framework.CommandProperties
import me.devoxin.jukebot.framework.Context

@CommandProperties(
    description = "Search and select from up to 5 tracks",
    aliases = ["search", "sel", "s", "find", "add"],
    category = CommandCategory.PLAYBACK
)
class Select : Command(ExecutionType.TRIGGER_CONNECT) {
    override fun execute(context: Context) {
        val player = context.audioPlayer

        if (!player.isPlaying) {
            player.channelId = context.channel.idLong
        }

        JukeBot.playerManager.loadIdentifier("${JukeBot.getSearchProvider()}:${context.args.gatherNext("query")}", context, player, true)
    }
}
