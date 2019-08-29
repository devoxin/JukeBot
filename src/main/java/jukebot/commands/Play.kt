package jukebot.commands

import jukebot.JukeBot
import jukebot.audio.AudioHandler
import jukebot.audio.SongResultHandler
import jukebot.framework.Command
import jukebot.framework.CommandCategory
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(description = "Finds and plays the provided song query/URL", aliases = ["p"], category = CommandCategory.PLAYBACK)
class Play : Command(ExecutionType.TRIGGER_CONNECT) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            player.channelId = context.channel.idLong
        }

        if (context.message.attachments.size > 0) {
            return loadWithAttachment(context, player)
        }

        loadWithArgs(context, player)
    }

    fun loadWithAttachment(ctx: Context, player: AudioHandler) {
        JukeBot.playerManager.loadItem(ctx.message.attachments[0].url, SongResultHandler(ctx, player, false))
    }

    fun loadWithArgs(ctx: Context, player: AudioHandler) {
        val manager = ctx.guild.audioManager
        val userQuery = ctx.argString.replace("[<>]".toRegex(), "")

        if (userQuery.startsWith("http")) {
            if (userQuery.toLowerCase().contains("/you/likes")) {
                ctx.embed("SoundCloud Liked Tracks", "JukeBot doesn't implement oauth and as a result\ncannot access your liked tracks when referenced as `you`")

                if (!player.isPlaying) {
                    manager.closeAudioConnection()
                }
                return
            }
            if (userQuery.toLowerCase().contains("pornhub") && !ctx.channel.isNSFW) {
                ctx.embed("PornHub Tracks", "PornHub tracks can only be loaded from NSFW channels!")

                if (!player.isPlaying) {
                    manager.closeAudioConnection()
                }

                return
            }
            JukeBot.playerManager.loadItem(userQuery, SongResultHandler(ctx, player, false))
        } else {
            JukeBot.playerManager.loadItem("ytsearch:$userQuery", SongResultHandler(ctx, player, false))
        }
    }
}
