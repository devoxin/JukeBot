package me.devoxin.jukebot.commands

import me.devoxin.flight.api.annotations.Choices
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Describe
import me.devoxin.flight.api.annotations.Range
import me.devoxin.flight.api.annotations.choice.StringChoice
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.entities.Cog
import me.devoxin.jukebot.annotations.Checks.DJ
import me.devoxin.jukebot.annotations.Checks.Playing
import me.devoxin.jukebot.annotations.Prerequisites.RequireMutualVoiceChannel
import me.devoxin.jukebot.extensions.audioPlayer
import me.devoxin.jukebot.extensions.embed
import me.devoxin.jukebot.utils.Helpers

class Configuration : Cog {
    @Command(description = "Configure track announcements.", guildOnly = true)
    @DJ(alone = false)
    fun announce(ctx: Context,
                 @Choices(string = [StringChoice("here", "here"), StringChoice("off", "off")])
                 @Describe("Where to send announcements.")
                 where: String) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server, so you can't currently configure this.")

        when (where.lowercase()) {
            "here" -> {
                player.channelId = ctx.messageChannel.idLong
                player.shouldAnnounce = true
                ctx.embed("Track Announcements", "This channel will now be used to for track announcements.")
            }
            "off" -> {
                player.shouldAnnounce = false
                ctx.embed("Track Announcements", "Track announcements are now disabled.")
            }
            else -> ctx.embed("Invalid Option", "You specified an invalid option. You can specify either `here` or `off`.")
        }
    }

    @Command(description = "Change the player volume.", guildOnly = true)
    @DJ(alone = false)
    @Playing
    @RequireMutualVoiceChannel
    fun volume(ctx: Context, @Describe("The new volume level. Omit to view.") @Range(long = [0, 250]) volume: Int?) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server, so you can't currently configure this.")

        if (volume == null) {
            val currentVolume = player.player.volume
            return ctx.embed("Player Volume", "${Helpers.createBar(currentVolume, 250, 10)} `$currentVolume%`")
        }

        player.player.volume = volume
        ctx.embed("Player Volume", "${Helpers.createBar(volume, 250, 10)} `$volume`")
    }
}
