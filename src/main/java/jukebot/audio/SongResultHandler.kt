package jukebot.audio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jukebot.JukeBot
import jukebot.framework.Context
import jukebot.utils.editEmbed
import jukebot.utils.toTimeString
import net.dv8tion.jda.api.EmbedBuilder
import java.util.regex.Pattern

class SongResultHandler(
        private val e: Context,
        private val musicManager: AudioHandler,
        private val useSelection: Boolean,
        private val playNext: Boolean = false
) : AudioLoadResultHandler {

    private val command = Pattern.compile("(?:p(?:lay)?|s(?:el(?:ect)?)?|sc(?:search)?|porn|spotify)\\s.+")

    private val playlistLimit: Int
        get() {
            if (JukeBot.isSelfHosted || e.donorTier >= 2)
                return Integer.MAX_VALUE

            return if (e.donorTier == 0) 100 else 1000
        }

    override fun trackLoaded(track: AudioTrack) {
        if (!canQueueTrack(track)) {
            e.embed("Track Unavailable", "This track exceeds certain limits. [Remove these limits by donating!](https://patreon.com/Devoxin)")
            return
        }

        if (musicManager.enqueue(track, e.author.idLong, playNext)) {
            e.embed("Track Enqueued", track.info.title)
        }
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        if (playlist.isSearchResult) {

            if (useSelection) {

                val selector = StringBuilder()

                val tracks = playlist.tracks
                        .filter { canQueueTrack(it) }
                        .take(5)

                if (tracks.isEmpty()) {
                    return noMatches()
                }

                for (i in tracks.indices) {
                    val track = tracks[i]
                    selector.append("`")
                            .append(i + 1)
                            .append(".` ")
                            .append(track.info.title)
                            .append(" `")
                            .append(track.duration.toTimeString())
                            .append("`\n")
                }

                e.channel.sendMessage(EmbedBuilder()
                        .setColor(e.embedColor)
                        .setTitle("Select Song")
                        .setDescription(selector.toString().trim())
                        .setFooter("Results are now filtered to display what you can queue", null)
                        .build()
                ).queue { m ->
                    JukeBot.waiter.waitForSelection(e.author.idLong, { selected ->
                        val s = selected?.toIntOrNull()

                        if (s == null || s <= 0 || s > tracks.size) {
                            m.delete().queue()

                            val manager = e.guild.audioManager

                            if (!musicManager.isPlaying && (selected == null || !command.matcher(selected.toLowerCase()).find())) {
                                manager.closeAudioConnection()
                            }

                            return@waitForSelection
                        }

                        val track = tracks[s - 1]

                        m.editEmbed {
                            setColor(e.embedColor)
                            setTitle("Track Selected")
                            setDescription(track.info.title)
                        }

                        musicManager.enqueue(track, e.author.idLong, false)
                    })
                }

            } else {
                if (playlist.tracks.isEmpty()) {
                    return noMatches()
                }

                val track = playlist.tracks[0]

                if (!canQueueTrack(track)) {
                    return e.embed("Track Unavailable", "This track exceeds certain limits. [Remove these limits by donating!](https://patreon.com/Devoxin)")
                }

                if (musicManager.enqueue(track, e.author.idLong, playNext)) {
                    e.embed("Track Enqueued", track.info.title)
                }
            }

        } else {

            val tracks = playlist.tracks
                    .filter { canQueueTrack(it) }
                    .take(playlistLimit)

            for (track in tracks) {
                musicManager.enqueue(track, e.author.idLong, false)
            }

            e.embed(playlist.name, "${tracks.size} tracks enqueued")
        }
    }

    override fun noMatches() {
        e.embed("No Results", "Nothing found related to the query.")

        if (!musicManager.isPlaying) {
            e.guild.audioManager.closeAudioConnection()
        }
    }

    override fun loadFailed(ex: FriendlyException) {
        e.embed("Track Unavailable", ex.localizedMessage)

        if (!musicManager.isPlaying) {
            e.guild.audioManager.closeAudioConnection()
        }
    }

    private fun canQueueTrack(track: AudioTrack): Boolean {
        val trackLength = Math.ceil((track.duration / 1000).toDouble()).toInt()
        var maxTrackDuration = 7500

        /* 7500 = ~ 2 hours
         * 18500 = ~ 5 hours
         */

        if (e.donorTier == 1) {
            maxTrackDuration = 18500
        } else if (e.donorTier >= 2) {
            maxTrackDuration = Integer.MAX_VALUE
        }

        return JukeBot.isSelfHosted || track.info.isStream && e.donorTier != 0 || trackLength <= maxTrackDuration
    }

}
