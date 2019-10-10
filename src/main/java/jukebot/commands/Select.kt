package jukebot.commands

import jukebot.JukeBot
import jukebot.framework.Command
import jukebot.framework.CommandCategory
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(
    description = "Search YouTube and select from up to 5 tracks",
    aliases = ["search", "sel", "s", "find", "add"],
    category = CommandCategory.PLAYBACK
)
class Select : Command(ExecutionType.TRIGGER_CONNECT) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            player.channelId = context.channel.idLong
        }

        JukeBot.playerManager.loadIdentifier("ytsearch:${context.argString}", context, player, true)
    }
}
