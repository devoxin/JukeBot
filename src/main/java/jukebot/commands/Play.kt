package jukebot.commands

import jukebot.JukeBot
import jukebot.audio.SongResultHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context

@CommandProperties(description = "Finds and plays the provided song query/URL", aliases = ["p"], category = CommandProperties.category.CONTROLS)
class Play : Command(ExecutionType.TRIGGER_CONNECT) {

    override fun execute(context: Context) {
        val manager = context.guild.audioManager
        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            player.channelId = context.channel.idLong
        }

        val userQuery = context.argString.replace("[<>]".toRegex(), "")

        if (userQuery.startsWith("http")) {
            if (userQuery.toLowerCase().contains("/you/likes")) {
                context.embed("SoundCloud Liked Tracks", "JukeBot doesn't implement oauth and as a result\ncannot access your liked tracks when referenced as `you`")

                if (!player.isPlaying) {
                    manager.closeAudioConnection()
                }
                return
            }
            if (userQuery.toLowerCase().contains("pornhub") && !context.channel.isNSFW) {
                context.embed("PornHub Tracks", "PornHub tracks can only be loaded from NSFW channels!")

                if (!player.isPlaying) {
                    manager.closeAudioConnection()
                }

                return
            }
            JukeBot.playerManager.loadItem(userQuery, SongResultHandler(context, player, false))
        } else {
            JukeBot.playerManager.loadItem("ytsearch:$userQuery", SongResultHandler(context, player, false))
        }

    }
}
