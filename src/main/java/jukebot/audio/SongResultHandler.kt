package jukebot.audio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jukebot.JukeBot
import jukebot.framework.Context
import jukebot.utils.editEmbed
import jukebot.utils.toTimeString
import kotlin.math.ceil

class SongResultHandler(
        private val ctx: Context,
        private val musicManager: AudioHandler,
        private val useSelection: Boolean,
        private val playNext: Boolean = false
) : AudioLoadResultHandler {

    private val playlistLimit: Int
        get() {
            return when {
                JukeBot.isSelfHosted -> Integer.MAX_VALUE
                ctx.donorTier >= 2 -> Integer.MAX_VALUE
                ctx.donorTier == 1 -> 1000
                else -> 100
            }
        }

    override fun trackLoaded(track: AudioTrack) {
        if (!canQueueTrack(track)) {
            ctx.embed("Track Unavailable", "This track exceeds certain limits. [Remove these limits by donating!](https://patreon.com/Devoxin)")
            return
        }

        if (musicManager.enqueue(track, ctx.author.idLong, playNext)) {
            ctx.embed("Track Enqueued", track.info.title)
        }
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        if (playlist.isSearchResult) {

            if (useSelection) {
                val menu = StringBuilder()

                val tracks = playlist.tracks
                        .filter { canQueueTrack(it) }
                        .take(5)

                if (tracks.isEmpty()) {
                    return noMatches()
                }

                for ((i, track) in tracks.withIndex()) {
                    menu.append("`${i + 1}.` ${track.info.title} `${track.duration.toTimeString()}`\n")
                }

                ctx.prompt("Select Song", menu.toString()) { m, it ->
                    val n = it?.toIntOrNull()

                    if (n == null || n <= 0 || n > tracks.size) {
                        m.delete().queue()

                        val manager = ctx.guild.audioManager

                        if (!musicManager.isPlaying && (it != null && !isCommand(it))) {
                            manager.closeAudioConnection()
                        }

                        return@prompt
                    }

                    val track = tracks[n - 1]

                    m.editEmbed {
                        setColor(ctx.embedColor)
                        setTitle("Track Selected")
                        setDescription(track.info.title)
                    }

                    musicManager.enqueue(track, ctx.author.idLong, false)
                }
            } else {
                val track = playlist.tracks.firstOrNull() ?: return noMatches()

                if (!canQueueTrack(track)) {
                    ctx.embed("Track Unavailable", "This track exceeds certain limits. [Remove these limits by donating!](https://patreon.com/Devoxin)")
                    return
                }

                if (musicManager.enqueue(track, ctx.author.idLong, playNext)) {
                    ctx.embed("Track Enqueued", track.info.title)
                }
            }
        } else {
            val tracks = playlist.tracks
                    .filter { canQueueTrack(it) }
                    .take(playlistLimit)

            for (track in tracks) {
                musicManager.enqueue(track, ctx.author.idLong, false)
            }

            ctx.embed(playlist.name, "${tracks.size} tracks enqueued")
        }
    }

    override fun noMatches() {
        ctx.embed("No Results", "Nothing found related to the query.")

        if (!musicManager.isPlaying) {
            ctx.guild.audioManager.closeAudioConnection()
        }
    }

    override fun loadFailed(ex: FriendlyException) {
        ctx.embed("Track Unavailable", ex.localizedMessage)

        if (!musicManager.isPlaying) {
            ctx.guild.audioManager.closeAudioConnection()
        }
    }

    private fun canQueueTrack(track: AudioTrack): Boolean {
        val trackLength = ceil((track.duration / 1000).toDouble()).toInt()
        var maxTrackDuration = 7500

        /* 7500 = ~ 2 hours
         * 18500 = ~ 5 hours
         */

        if (ctx.donorTier == 1) {
            maxTrackDuration = 18500
        } else if (ctx.donorTier >= 2) {
            maxTrackDuration = Integer.MAX_VALUE
        }

        return JukeBot.isSelfHosted || track.info.isStream && ctx.donorTier != 0 || trackLength <= maxTrackDuration
    }

    fun isCommand(s: String): Boolean {
        if (!s.contains(" ")) {
            return false
        }

        val prefix = setOf(ctx.prefix, ctx.guild.selfMember.asMention, ctx.jda.selfUser.asMention)
                .firstOrNull { s.startsWith(it) } ?: return false

        val ct = s.substring(prefix.length).trim()
        return commands.any { ct.startsWith(it) }
    }

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
