package jukebot.commands.playback

import jukebot.JukeBot
import jukebot.audio.AudioHandler
import jukebot.framework.Command
import jukebot.framework.CommandCategory
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(
    description = "Finds and plays the provided song query/URL",
    aliases = ["p"],
    category = CommandCategory.PLAYBACK
)
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

    private fun loadWithAttachment(ctx: Context, player: AudioHandler) {
        val identifier = ctx.message.attachments[0].url
        JukeBot.playerManager.loadIdentifier(identifier, ctx, player, false)
    }

    private fun loadWithArgs(ctx: Context, player: AudioHandler) {
        val manager = ctx.guild.audioManager
        val userQuery = ctx.argString.removePrefix("<").removeSuffix(">")

        if (userQuery.startsWith("http") || userQuery.startsWith("spotify:")) {
            if ("soundcloud.com/you/" in userQuery.lowercase()) {
                ctx.embed("SoundCloud Liked Tracks", "Loading SoundCloud tracks requires username.")

                if (!player.isPlaying) {
                    manager.closeAudioConnection()
                }

                return
            }

            if ("pornhub" in userQuery.lowercase() && !ctx.channel.isNSFW) {
                ctx.embed("PornHub Tracks", "PornHub tracks can only be loaded from NSFW channels!")

                if (!player.isPlaying) {
                    manager.closeAudioConnection()
                }

                return
            }

            val url = userQuery.split(' ')
            JukeBot.playerManager.loadIdentifier(url[0], ctx, player, false)
        } else {
            JukeBot.playerManager.loadIdentifier("ytsearch:$userQuery", ctx, player, false)
        }
    }
}
