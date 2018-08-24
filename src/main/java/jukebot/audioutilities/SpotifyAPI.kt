package jukebot.audioutilities

import jukebot.JukeBot
import jukebot.utils.Helpers
import jukebot.utils.json
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class SpotifyAPI(private val clientId: String, private val clientSecret: String) {

    private val httpClient: OkHttpClient = OkHttpClient()
    private var accessToken: String = ""

    init {
        refreshAccessToken()
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

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                JukeBot.LOG.warn("[SpotifyAPI] Unable to update Spotify access token!", e)
                Helpers.schedule({ refreshAccessToken() }, 1, TimeUnit.MINUTES)
            }

            override fun onResponse(call: Call, response: Response) {
                val json = response.json()

                if (json == null) {
                    JukeBot.LOG.warn("[SpotifyAPI] Response body was null!", response.code(), response.message())
                    return Helpers.schedule({ refreshAccessToken() }, 1, TimeUnit.MINUTES)
                }

                if (json.has("error") && json.getString("error").startsWith("invalid_")) {
                    return JukeBot.LOG.error("[SpotifyAudioSource] Spotify API access disabled (${json.getString("error")})")
                }

                val refreshIn = json.getInt("expires_in")

                accessToken = json.getString("access_token")
                Helpers.schedule({ refreshAccessToken() }, (refreshIn * 1000) - 10000, TimeUnit.MILLISECONDS)

                JukeBot.LOG.info("[SpotifyAudioSource] Updated access token to $accessToken")
            }
        })
    }

    fun getTracksFromPlaylist(userId: String, playlistId: String, callback: (SpotifyPlaylist?) -> Unit) {
        val request = Request.Builder()
                .url("https://api.spotify.com/v1/users/$userId/playlists/$playlistId/tracks")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val json = response.json() ?: return callback(null)

                val playlist = SpotifyPlaylist()

                if (!json.has("items")) {
                    return callback(null)
                }

                val tracks = json.getJSONArray("items")

                tracks.forEach {
                    val t = (it as JSONObject).getJSONObject("track")
                    val artist = t.getJSONArray("artists").getJSONObject(0).getString("name")
                    val trackName = t.getString("name")
                    playlist.addTrack(artist, trackName)
                }

                callback(playlist)
            }
        })
    }

    fun getTracksFromPlaylistBlocking(userId: String, playlistId: String): SpotifyPlaylist? {
        val promise = CompletableFuture<SpotifyPlaylist?>()

        getTracksFromPlaylist(userId, playlistId) {
            promise.complete(it)
        }

        return promise.get(30, TimeUnit.SECONDS)
    }

}

class SpotifyPlaylist(val name: String = "Spotify Playlist") {

    val tracks: MutableList<SpotifyAudioTrack> = ArrayList()

    fun addTrack(artist: String, title: String) {
        tracks.add(SpotifyAudioTrack(artist, title))
    }

}

class SpotifyAudioTrack(val artist: String, val title: String)
