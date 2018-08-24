package jukebot.audioutilities

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.track.*
import jukebot.JukeBot
import java.io.DataInput
import java.io.DataOutput
import java.lang.UnsupportedOperationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import java.util.regex.Pattern


class SpotifyAudioSourceManager(private val sApi: SpotifyAPI, poolSize: Int) : AudioSourceManager {

    private var loaderPool = Executors.newFixedThreadPool(poolSize)!!

    override fun getSourceName(): String {
        return "spotify"
    }

    override fun loadItem(manager: DefaultAudioPlayerManager, reference: AudioReference): AudioItem? {
        if (!reference.identifier.startsWith("spotify:") || !sApi.isEnabled()) {
            return null
        }

        val match = PLAYLIST_PATTERN.matcher(reference.identifier.substring(8))

        if (!match.matches()) {
            return null
        }

        val userId: String = match.group(1)
        val listId: String = match.group(2)

        return try {
            JukeBot.LOG.debug("Loading Spotify playlist with identifier $listId")
            loadItemOnce(userId, listId)
        } catch (exception: FriendlyException) {
            // In case of a connection reset exception, try once more.
            if (HttpClientTools.isRetriableNetworkException(exception.cause)) {
                loadItemOnce(userId, listId)
            } else {
                throw exception
            }
        }

    }

    override fun isTrackEncodable(track: AudioTrack): Boolean {
        return false
    }

    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        throw UnsupportedOperationException("Source manager may only be used to load playlists")
    }

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack {
        throw UnsupportedOperationException("Source manager may only be used to load playlists")
    }

    override fun shutdown() {
        loaderPool.shutdown()
    }

    private fun loadItemOnce(spotifyUser: String, listId: String): AudioItem {
        val playlist = sApi.getTracksFromPlaylistBlocking(spotifyUser, listId) ?: return AudioReference.NO_TRACK
        val trackTasks = ArrayList<CompletableFuture<AudioTrack?>>()
        val resolvedTracks = ArrayList<AudioTrack>()

        playlist.tracks.forEach {
            val task = CompletableFuture.supplyAsync(Supplier { JukeBot.youTubeApi.searchVideoBlocking("${it.title} ${it.artist}") }, loaderPool)
            trackTasks.add(task)
        }

        trackTasks.forEach {
            val track = it.get(5, TimeUnit.SECONDS) ?: return@forEach
            resolvedTracks.add(track)
        }

        return BasicAudioPlaylist(playlist.name, resolvedTracks, null, false)
    }

    companion object {
        private val PLAYLIST_PATTERN = Pattern.compile("^https?://.*\\.spotify\\.com/user/([a-zA-Z0-9_]+)/playlist/([a-zA-Z0-9]+).*")
    }

}
