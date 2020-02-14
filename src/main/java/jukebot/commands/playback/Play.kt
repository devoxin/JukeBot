package jukebot.commands.playback

import jukebot.JukeBot
import jukebot.audio.AudioHandler
import jukebot.audio.sourcemanagers.spotify.SpotifyAudioSourceManager
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

    private fun loadWithAttachment(ctx: Context, player: AudioHandler) {
        val identifier = ctx.message.attachments[0].url
        JukeBot.playerManager.loadIdentifier(identifier, ctx, player, false)
    }

    private fun loadWithArgs(ctx: Context, player: AudioHandler) {
        val manager = ctx.guild.audioManager
        val userQuery = ctx.argString.replace("[<>]".toRegex(), "")

        if (userQuery.startsWith("http")) {
            if (userQuery.toLowerCase().contains("soundcloud.com/you/")) {
                ctx.embed("SoundCloud Liked Tracks", "Loading SoundCloud tracks requires username.")

                if (!player.isPlaying) {
                    manager.closeAudioConnection()
                }

                return
            }

            if (!JukeBot.isYoutubeEnabled &&
                userQuery.toLowerCase().contains("youtube") || userQuery.toLowerCase().contains("youtu.be")) {
                ctx.embed("YouTube Support", "YouTube support is unavailable.")
                return
            }

            if (userQuery.toLowerCase().contains("pornhub") && !ctx.channel.isNSFW) {
                ctx.embed("PornHub Tracks", "PornHub tracks can only be loaded from NSFW channels!")

                if (!player.isPlaying) {
                    manager.closeAudioConnection()
                }

                return
            }

            if (SpotifyAudioSourceManager.isSpotifyMedia(userQuery)) {
                val customIdentifier = "s!$userQuery!${ctx.donorTier}"
                JukeBot.playerManager.loadIdentifier(customIdentifier, userQuery, ctx, player, false)
            } else {
                JukeBot.playerManager.loadIdentifier(userQuery, ctx, player, false)
            }
        } else {
            JukeBot.playerManager.loadIdentifier(SEARCH_TYPE + userQuery, ctx, player, false)
        }
    }

    companion object {
        val SEARCH_TYPE = if (JukeBot.isYoutubeEnabled) "ytsearch:" else "scsearch:"
    }
}
