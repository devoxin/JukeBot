package jukebot.audio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import io.sentry.Sentry
import jukebot.JukeBot
import jukebot.audio.sourcemanagers.caching.CachingSourceManager
import jukebot.framework.Context
import jukebot.utils.Helpers
import jukebot.utils.Limits
import jukebot.utils.editEmbed
import jukebot.utils.toTimeString
import org.jetbrains.kotlin.utils.addToStdlib.sumByLong
import java.util.concurrent.TimeUnit

class SongResultHandler(
    private val ctx: Context,
    private val identifier: String,
    private val musicManager: AudioHandler,
    private val useSelection: Boolean,
    private val playNext: Boolean = false
) : AudioLoadResultHandler {

    override fun trackLoaded(track: AudioTrack) {
        cache(track)

        if (!canQueueTrack(track)) {
            ctx.embed("Track Unavailable", "This track exceeds certain limits. [Remove these limits by donating!](https://patreon.com/Devoxin)")
            return
        }

        val estPlay = calculateEstimatedPlayTime()

        if (musicManager.enqueue(track, ctx.author.idLong, playNext)) {
            ctx.embed {
                setTitle("Track Enqueued")
                setDescription(track.info.title)
                setFooter("Estimated time until play: ${estPlay.toTimeString()}")
            }
        }
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        cache(playlist)

        if (playlist.isSearchResult) {
            if (useSelection) {
                val menu = StringBuilder()

                val tracks = playlist.tracks
                    .filter(::canQueueTrack)
                    .take(5)

                if (tracks.isEmpty()) {
                    return noMatches()
                }

                for ((i, track) in tracks.withIndex()) {
                    menu.append("`${i + 1}.` ${track.info.title} `${track.duration.toTimeString()}`\n")
                }

                ctx.prompt("Select Song", menu.toString()) { m, it ->
                    val n = it?.toIntOrNull()?.takeIf { it > 0 && it <= tracks.size }

                    if (n == null) {
                        m.delete().queue()

                        val manager = ctx.guild.audioManager

                        if (!musicManager.isPlaying && (it != null && !isCommand(it))) {
                            manager.closeAudioConnection()
                        }

                        return@prompt
                    }

                    val estPlay = calculateEstimatedPlayTime()

                    val track = tracks[n - 1]

                    m.editEmbed {
                        setColor(ctx.embedColor)
                        setTitle("Track Selected")
                        setDescription(track.info.title)

                        if (estPlay > 0) {
                            setFooter("Estimated time until play: ${estPlay.toTimeString()}")
                        }
                    }

                    musicManager.enqueue(track, ctx.author.idLong, false)
                }
            } else {
                val track = playlist.tracks.firstOrNull() ?: return noMatches()

                if (!canQueueTrack(track)) {
                    ctx.embed("Track Unavailable", "This track exceeds certain limits. [Remove these limits by donating!](https://patreon.com/Devoxin)")
                    return
                }

                val estPlay = calculateEstimatedPlayTime()

                if (musicManager.enqueue(track, ctx.author.idLong, playNext)) {
                    ctx.embed {
                        setTitle("Track Enqueued")
                        setDescription(track.info.title)
                        setFooter("Estimated time until play: ${estPlay.toTimeString()}")
                    }
                }
            }
        } else {
            val tracks = playlist.tracks
                .filter(::canQueueTrack)
                .take(Limits.playlist(ctx.donorTier))

            var estPlay = musicManager.queue.sumByLong { it.duration }

            if (musicManager.current != null) {
                estPlay += musicManager.current!!.duration - musicManager.current!!.position
            }

            for (track in tracks) {
                musicManager.enqueue(track, ctx.author.idLong, false)
            }

            ctx.embed {
                setTitle(playlist.name)
                setDescription("Enqueued **${tracks.size}** tracks")

                if (estPlay > 0) {
                    setFooter("Estimated time until play: ${estPlay.toTimeString()}")
                }
            }
        }
    }

    override fun noMatches() {
        ctx.embed("No Results", "Nothing found related to the query.")

        if (!musicManager.isPlaying) {
            ctx.guild.audioManager.closeAudioConnection()
        }
    }

    override fun loadFailed(ex: FriendlyException) {
        Sentry.capture(ex)
        ctx.embed("Track Unavailable", Helpers.rootCauseOf(ex).localizedMessage)

        if (!musicManager.isPlaying) {
            ctx.guild.audioManager.closeAudioConnection()
        }
    }

    private fun calculateEstimatedPlayTime(): Long {
        val current = musicManager.current
        val remaining = current?.duration?.minus(current.position) ?: 0L
        return if (playNext) remaining else musicManager.queue.sumByLong { it.duration } + remaining
    }

    private fun canQueueTrack(track: AudioTrack): Boolean {
        val maxTrackDuration = Limits.duration(ctx.donorTier)
        return JukeBot.isSelfHosted || track.info.isStream && ctx.donorTier > 0 || maxTrackDuration >= track.duration
    }

    private fun isCommand(s: String): Boolean {
        if (!s.contains(" ")) {
            return false
        }

        val prefix = setOf(ctx.prefix, ctx.guild.selfMember.asMention, ctx.jda.selfUser.asMention)
            .firstOrNull { s.startsWith(it) } ?: return false

        val ct = s.substring(prefix.length).trim()
        return commands.any { ct.startsWith(it) }
    }

    fun cache(item: AudioItem) = CachingSourceManager.cache(identifier, item)

    companion object {
        private val commands = listOf(
            "p", "play", "playrelated", "pr", "playnext", "pn",
            "s", "sel", "select",
            "sc", "scsearch",
            "porn",
            "spotify"
        )
    }

}
