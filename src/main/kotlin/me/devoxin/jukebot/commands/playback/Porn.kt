package me.devoxin.jukebot.commands.playback

import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.framework.Command
import me.devoxin.jukebot.framework.CommandCategory
import me.devoxin.jukebot.framework.CommandProperties
import me.devoxin.jukebot.framework.Context
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel

@CommandProperties(description = "Searches for a track on PornHub and queues it", nsfw = true, category = CommandCategory.PLAYBACK)
class Porn : Command(ExecutionType.TRIGGER_CONNECT) {
    override fun execute(context: Context) {
        val channel = context.channel

        if (!(channel is VoiceChannel && channel.isNSFW) && !(channel is TextChannel && channel.isNSFW)) {
            return context.embed("PornHub Search", "Searches can only be performed from NSFW channels.")
        }

        if ((context.member.voiceState?.channel as? VoiceChannel)?.isNSFW != true) {
            return context.embed("PornHub Search", "PornHub audio can only be played in NSFW voice channels.")
        }

        val player = context.audioPlayer

        if (!player.isPlaying) {
            player.channelId = context.channel.idLong
        }

        JukeBot.playerManager.loadIdentifier("phsearch:${context.argString}", context, player, true)
    }
}
