package me.devoxin.jukebot.commands

import me.devoxin.flight.api.annotations.Choices
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Describe
import me.devoxin.flight.api.annotations.choice.StringChoice
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.entities.Cog
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.annotations.Checks.DJ
import me.devoxin.jukebot.annotations.Checks.Playing
import me.devoxin.jukebot.annotations.Prerequisites.RequireMutualVoiceChannel
import me.devoxin.jukebot.audio.AudioHandler.RepeatMode.*
import me.devoxin.jukebot.extensions.audioPlayer
import me.devoxin.jukebot.extensions.embed
import me.devoxin.jukebot.extensions.isDJ
import me.devoxin.jukebot.extensions.parseAsTimeStringToMillisecondsOrNull
import kotlin.math.ceil

class Controls : Cog {
    @Command(aliases = ["rewind", "rs", "rw"], description = "Restart the current track.", guildOnly = true)
    @DJ(alone = true)
    @Playing
    fun restart(ctx: Context) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        val track = player.player.playingTrack
            ?: return ctx.embed("Not Playing", "There's nothing playing.")

        if (!track.isSeekable) {
            return ctx.embed("Track Seeking", "The current track doesn't support seeking.")
        }

        ctx.asSlashContext?.respond({
            setContent("Back to the beginning!")
        }, true)

        track.position = 0
    }

    @Command(aliases = ["fs"], description = "Skip the current track without voting.", guildOnly = true)
    @Playing
    @RequireMutualVoiceChannel
    fun forceskip(ctx: Context) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        val track = player.player.playingTrack
            ?: return ctx.embed("Not Playing", "There's nothing playing.")

        if (!ctx.isDJ(true) && track.userData as Long != ctx.author.idLong) {
            return ctx.embed("Not a DJ", "You need to be a DJ to use this command.")
        }

        player.next()
        ctx.asSlashContext?.reply("Skipped.", true)
    }

    @Command(description = "Pause the audio player.", guildOnly = true)
    @DJ(alone = true)
    @Playing
    @RequireMutualVoiceChannel
    fun pause(ctx: Context) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        player.player.isPaused = true
        ctx.asSlashContext?.reply("Paused.", true)
    }

    @Command(description = "Resume the audio player.", guildOnly = true)
    @DJ(alone = true)
    @Playing
    @RequireMutualVoiceChannel
    fun resume(ctx: Context) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        player.player.isPaused = false
        ctx.asSlashContext?.reply("Resumed.", true)
    }

    @Command(description = "Toggle queue shuffling.", guildOnly = true)
    @DJ(alone = true)
    @Playing
    @RequireMutualVoiceChannel
    fun shuffle(ctx: Context) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        player.shuffle = !player.shuffle
        ctx.embed("Queue Shuffling", if (player.shuffle) "The queue will now play in a random order." else "The queue will play sequentially.")
    }

    @Command(description = "Toggle track/queue repeating.", guildOnly = true)
    @DJ(alone = true)
    @Playing
    @RequireMutualVoiceChannel
    fun repeat(ctx: Context,
               @Choices(string = [StringChoice("queue / all", "all"), StringChoice("track / current", "single"), StringChoice("off / none", "none")])
               @Describe("What to repeat")
               setting: String) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        when (setting) {
            "a", "all", "q", "queue" -> player.repeat = ALL
            "s", "single", "c", "current", "t", "track" -> player.repeat = SINGLE
            "n", "none", "o", "off" -> player.repeat = NONE
            else -> return ctx.embed("Player Repeat", "That's not a valid setting. You can choose from `all`, `single` or `none`.")
        }

        ctx.embed("Player Repeat", "The repeat setting has been changed to `${player.repeat.humanized()}`.")
    }

    @Command(aliases = ["next"], description = "Vote to skip the current track.", guildOnly = true)
    @Playing
    @RequireMutualVoiceChannel
    fun skip(ctx: Context) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        val totalVotes = player.voteSkip(ctx.author.idLong)
        val voteThreshold = Database.getSkipThreshold(ctx.guild!!.idLong)
        val neededVotes = ceil(ctx.guild!!.audioManager.connectedChannel!!.members.count { !it.user.isBot } * voteThreshold).toInt()

        if (totalVotes >= neededVotes) {
            ctx.embed("Vote Skip", "Voting has passed. The track will be skipped.")
            player.next()
        } else {
            ctx.embed("Vote Skip", "Your vote has been counted.\n${(neededVotes - totalVotes)} votes needed to skip.")
        }
    }

    @Command(description = "Stop the music and clear the queue.", guildOnly = true)
    @DJ(alone = true)
    @Playing
    @RequireMutualVoiceChannel
    fun stop(ctx: Context) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        player.repeat = NONE
        player.queue.clear()
        player.next(false)

        ctx.asSlashContext?.reply("The music has been stopped, and the queue has been cleared.", true)
    }

    @Command(aliases = ["jump"], description = "Seek to a position in the current track.", guildOnly = true)
    @DJ(alone = true)
    @Playing
    fun seek(ctx: Context,
             @Describe("A timestamp, or the number of seconds (can be negative)") time: String) {
        val track = ctx.audioPlayer?.player?.playingTrack
            ?: return ctx.embed("Not Playing", "There's nothing playing.")

        if (!track.isSeekable) {
            return ctx.embed("Track Seeking", "The current track doesn't support seeking.")
        }

        val seekPosition = time.parseAsTimeStringToMillisecondsOrNull()
            ?: return ctx.embed("Track Seeking", "That's not a valid time. You can specify a timestamp such as `mm:ss`/`hh:mm:ss` or the number of seconds to seek.")

        track.position += seekPosition
        ctx.asSlashContext?.reply("Track seeking has been requested.", true)
    }
}
