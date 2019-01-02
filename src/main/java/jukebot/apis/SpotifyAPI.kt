package jukebot.apis

import jukebot.JukeBot
import jukebot.utils.Helpers
import jukebot.utils.json
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class SpotifyAPI(private val clientId: String, private val clientSecret: String) {

    private var accessToken: String = ""

    init {
        if (credentialsProvided()) {
            refreshAccessToken()
        }
    }

    fun credentialsProvided(): Boolean {
        return clientId.isNotBlank() && clientSecret.isNotBlank()
    }

    fun isEnabled(): Boolean {
        return credentialsProvided() && accessToken.isNotBlank()
    }

    private fun refreshAccessToken() {
        val base64Auth = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())
        val body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), "grant_type=client_credentials")

        val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .addHeader("Authorization", "Basic $base64Auth")
                .post(body)
                .build()

        JukeBot.httpClient.makeRequest(request).queue({
            val json = it.json()

            if (json == null) {
                JukeBot.LOG.warn("[SpotifyAPI] Response body was null!", it.code(), it.message())
                Helpers.schedule({ refreshAccessToken() }, 1, TimeUnit.MINUTES)
                return@queue
            }

            if (json.has("error") && json.getString("error").startsWith("invalid_")) {
                JukeBot.LOG.error("[SpotifyAudioSource] Spotify API access disabled (${json.getString("error")})")
                return@queue
            }

            val refreshIn = json.getInt("expires_in")

            accessToken = json.getString("access_token")
            Helpers.schedule({ refreshAccessToken() }, (refreshIn * 1000) - 10000, TimeUnit.MILLISECONDS)

            JukeBot.LOG.info("[SpotifyAudioSource] Updated access token to $accessToken")
        }, {
            JukeBot.LOG.warn("[SpotifyAPI] Unable to update Spotify access token!", it)
            Helpers.schedule({ refreshAccessToken() }, 1, TimeUnit.MINUTES)
        })
    }

    fun getTracksFromPlaylist(userId: String, playlistId: String, callback: (SpotifyPlaylist?) -> Unit) {
        val request = Request.Builder()
                .url("https://api.spotify.com/v1/users/$userId/playlists/$playlistId/tracks")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

        JukeBot.httpClient.makeRequest(request).queue({
            val json = it.json() ?: return@queue callback(null)
            val playlist = SpotifyPlaylist()

            if (!json.has("items")) {
                return@queue callback(null)
            }

            val tracks = json.getJSONArray("items")

            tracks.forEach {
                val t = (it as JSONObject).getJSONObject("track")
                val artist = t.getJSONArray("artists").getJSONObject(0).getString("name")
                val trackName = t.getString("name")
                playlist.addTrack(artist, trackName)
            }

            callback(playlist)
        }, {
            callback(null)
        })
    }

    fun getTracksFromPlaylistBlocking(userId: String, playlistId: String): SpotifyPlaylist? {
        val promise = CompletableFuture<SpotifyPlaylist?>()

        getTracksFromPlaylist(userId, playlistId) {
            promise.complete(it)
        }

        return promise.get(30, TimeUnit.SECONDS)
    }

    fun search(title: String): CompletableFuture<SpotifyAudioTrack?> {
        val request = Request.Builder()
                .url("https://api.spotify.com/v1/search?q=$title&type=track")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

        val future = CompletableFuture<SpotifyAudioTrack?>()

        JukeBot.httpClient.makeRequest(request).queue({
            val json = it.json()

            if (json == null) {
                future.complete(null)
                return@queue
            }

            val results = json.getJSONObject("tracks").getJSONArray("items")

            if (results.length() == 0) {
                future.complete(null)
                return@queue
            }

            val track = results.getJSONObject(0)
            val artist = track.getJSONArray("artists").getJSONObject(0).getString("name")
            val trackTitle = track.getString("name")

            future.complete(SpotifyAudioTrack(artist, trackTitle))
        }, {
            future.complete(null)
        })

        return future
    }
}

class SpotifyPlaylist(val name: String = "Spotify Playlist") {

    val tracks: MutableList<SpotifyAudioTrack> = ArrayList()

    fun addTrack(artist: String, title: String) {
        tracks.add(SpotifyAudioTrack(artist, title))
    }

}

class SpotifyAudioTrack(val artist: String, val title: String)
