package me.devoxin.jukebot.commands.playback

import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.audio.AudioHandler
import me.devoxin.jukebot.framework.Command
import me.devoxin.jukebot.framework.CommandCategory
import me.devoxin.jukebot.framework.CommandProperties
import me.devoxin.jukebot.framework.Context
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel

@CommandProperties(
    description = "Finds and plays the provided song query/URL",
    aliases = ["p"],
    category = CommandCategory.PLAYBACK
)
class Play : Command(ExecutionType.TRIGGER_CONNECT) {
    override fun execute(context: Context) {
        val player = context.audioPlayer

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

                if (!player.isPlaying && player.queue.isEmpty()) {
                    manager.closeAudioConnection()
                }

                return
            }

            if ("pornhub" in userQuery.lowercase()) {
                val nsfwTcCheck = (ctx.channel as? VoiceChannel)?.isNSFW == true || (ctx.channel as? TextChannel)?.isNSFW == true
                val nsfwVcCheck = (ctx.member.voiceState?.channel as? VoiceChannel)?.isNSFW == true

                if (!nsfwTcCheck || !nsfwVcCheck) {
                    when {
                        !nsfwTcCheck -> ctx.embed("PornHub Tracks", "PornHub tracks can only be loaded from NSFW channels!")
                        !nsfwVcCheck -> ctx.embed("PornHub Tracks", "PornHub tracks can only be played in NSFW voice channels!")
                    }
                    // double-checking is useless but I cba to figure out a way to make this better lol

                    if (!player.isPlaying && player.queue.isEmpty()) {
                        manager.closeAudioConnection()
                    }

                    return
                }
            }

            val url = userQuery.split(' ')
            JukeBot.playerManager.loadIdentifier(url[0], ctx, player, false)
        } else {
            JukeBot.playerManager.loadIdentifier("${JukeBot.getSearchProvider()}:$userQuery", ctx, player, false)
        }
    }
}
