package me.devoxin.jukebot.commands.playback

import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.framework.Command
import me.devoxin.jukebot.framework.CommandCategory
import me.devoxin.jukebot.framework.CommandProperties
import me.devoxin.jukebot.framework.Context

@CommandProperties(description = "Search SoundCloud and queue the top result", aliases = ["sc"], category = CommandCategory.PLAYBACK)
class ScSearch : Command(ExecutionType.TRIGGER_CONNECT) {
    override fun execute(context: Context) {
        val player = context.audioPlayer

        if (!player.isPlaying) {
            player.channelId = context.channel.idLong
        }

        JukeBot.playerManager.loadIdentifier("scsearch:${context.args.gatherNext("query")}", context, player, false)
    }
}
