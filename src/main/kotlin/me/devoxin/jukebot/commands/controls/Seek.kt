package me.devoxin.jukebot.commands.controls

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.devoxin.jukebot.audio.AudioHandler
import me.devoxin.jukebot.framework.*
import me.devoxin.jukebot.utils.toTimeString
import net.dv8tion.jda.api.interactions.commands.OptionType

@CommandProperties(
    description = "Jump to a certain time, or by a specific amount of seconds.",
    category = CommandCategory.CONTROLS,
    aliases = ["jump"],
    slashCompatible = true
)
@Option(name = "time", description = "A timestamp, or number of seconds to jump.", type = OptionType.STRING)
@CommandChecks.Dj(alone = true)
@CommandChecks.Playing
class Seek : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val player = context.audioPlayer
        val currentTrack = player.player.playingTrack

        if (!currentTrack.isSeekable) {
            return context.embed("Seek Unavailable", "The current track doesn't support seeking.")
        }

        val arg = context.args.next("time", ArgumentResolver.STRING)
            ?: return context.embed("Track Seeking", "You need to specify a time, or amount of seconds to jump by.")

        if (arg.contains(":")) {
            parseTime(context, player, currentTrack, arg)
        } else {
            parseSeconds(context, player, currentTrack, arg)
        }
    }

    private fun parseSeconds(ctx: Context, player: AudioHandler, track: AudioTrack, seconds: String) {
        val jumpTime = seconds.toIntOrNull()
            ?: return ctx.embed("Track Seeking", "You need to specify a valid amount of seconds to jump.")

        val jumpTimeMs = jumpTime * 1000

        if (track.position + jumpTimeMs >= track.duration) {
            return player.playNext()
        }

        track.position += jumpTimeMs
        ctx.embed("Track Seeking", "Seeked to **${track.position.toTimeString()}**/${track.info.length.toTimeString()}")
    }

    private fun parseTime(ctx: Context, player: AudioHandler, track: AudioTrack, time: String) {
        val parts = time.split(":").map { it.toIntOrNull() }

        if (parts.any { it == null }) {
            return ctx.embed("Track Seeking", "Invalid time specified.\nExamples: `02:33`, `01:18:29`, `01:08:29:56`")
        }

        val partsInt = parts.filterNotNull() // None should be null, but IntelliJ is complaining.

        val jumpTimeMs = when (parts.size) {
            4 -> { // DD:HH:MM:SS
                val (days, hours, minutes, seconds) = partsInt
                (days * 86400) + (hours * 3600) + (minutes * 60) + seconds
            }
            3 -> { // HH:MM:SS
                val (hours, minutes, seconds) = partsInt
                (hours * 3600) + (minutes * 60) + seconds
            }
            2 -> { // MM:SS
                val (minutes, seconds) = partsInt
                (minutes * 60) + seconds
            }
            else -> return ctx.embed(
                "Track Seeking",
                "Invalid time.\nAcceptable formats: `mm:ss`, `hh:mm:ss`, `dd:hh:mm:ss`"
            )
        }

        if (track.position + jumpTimeMs >= track.duration) {
            return player.playNext()
        }

        track.position = jumpTimeMs.toLong() * 1000
        ctx.embed("Track Seeking", "Seeked to **${track.position.toTimeString()}**/${track.info.length.toTimeString()}")
    }
}
