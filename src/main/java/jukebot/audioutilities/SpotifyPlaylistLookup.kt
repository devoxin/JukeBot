package jukebot.audioutilities

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jukebot.JukeBot
import java.util.concurrent.CompletableFuture

class SpotifyPlaylistLookup(val tracks: List<SparseSpotifyAudioTrack>) {

    fun resolveTracks(): MutableList<AudioTrack?> {
        val resolved: MutableList<AudioTrack?> = ArrayList()
        val promise = CompletableFuture<MutableList<AudioTrack?>>()

        for (track in tracks) {
            JukeBot.youTubeApi.searchVideo("${track.name} ${track.artist} lyrics", {
                if (it == null) {
                    return@searchVideo
                }

                JukeBot.playerManager.loadItem(it, object : AudioLoadResultHandler {
                    override fun trackLoaded(track: AudioTrack) {
                        resolved.add(track)
                        checkPromise()
                    }

                    override fun playlistLoaded(playlist: AudioPlaylist) {
                        if (!playlist.tracks.isEmpty()) {
                            resolved.add(playlist.tracks[0])
                        } else {
                            resolved.add(null)
                        }

                        checkPromise()
                    }

                    override fun noMatches() {
                        resolved.add(null)
                        checkPromise()
                    }

                    override fun loadFailed(ex: FriendlyException) {
                        resolved.add(null)
                        checkPromise()
                    }

                    fun checkPromise() {
                        if (resolved.size == tracks.size) {
                            promise.complete(resolved)
                        }
                    }
                })
            })
        }

        return promise.get()
    }

}
