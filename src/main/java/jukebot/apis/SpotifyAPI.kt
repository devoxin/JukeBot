package jukebot.apis

import jukebot.JukeBot
import jukebot.entities.spotify.SpotifyAudioTrack
import jukebot.entities.spotify.SpotifyPlaylist
import jukebot.utils.Helpers
import jukebot.utils.json
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class SpotifyAPI(private val clientId: String, private val clientSecret: String) {

    private var accessToken = ""

    init {
        if (credentialsProvided()) {
            refreshAccessToken()
        }
    }

    fun credentialsProvided() = clientId.isNotBlank() && clientSecret.isNotBlank()

    fun isEnabled() = credentialsProvided() && accessToken.isNotBlank()

    private fun refreshAccessToken() {
        val base64Auth = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())
        val body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), "grant_type=client_credentials")

        val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .addHeader("Authorization", "Basic $base64Auth")
                .post(body)
                .build()

        makeRequest(request)
                .thenAccept {
                    if (it.has("error") && it.getString("error").startsWith("invalid_")) {
                        JukeBot.LOG.error("[SpotifyAudioSource] Spotify API access disabled (${it.getString("error")})")
                        return@thenAccept
                    }

                    val refreshIn = it.getInt("expires_in")

                    accessToken = it.getString("access_token")
                    Helpers.schedule({ refreshAccessToken() }, (refreshIn * 1000) - 10000, TimeUnit.MILLISECONDS)

                    JukeBot.LOG.info("[SpotifyAudioSource] Updated access token to $accessToken")
                }
                .exceptionally {
                    JukeBot.LOG.warn("[SpotifyAPI] Error occurred while refreshing access token!", it)
                    Helpers.schedule({ refreshAccessToken() }, 1, TimeUnit.MINUTES)
                    return@exceptionally null
                }
    }

    fun getPlaylistInfo(playlistId: String): JSONObject {
        val request = Request.Builder()
                .url("https://api.spotify.com/v1/playlists/$playlistId")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

        return makeRequest(request).get()
    }

    fun getPlaylist(playlistId: String): CompletableFuture<SpotifyPlaylist> {
        val request = Request.Builder()
                .url("https://api.spotify.com/v1/playlists/$playlistId/tracks")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

        val future = CompletableFuture<SpotifyPlaylist>()

        makeRequest(request)
                .thenAccept {
                    val info = getPlaylistInfo(playlistId)
                    val playlistName = info.getString("name")

                    if (!it.has("items")) {
                        future.complete(SpotifyPlaylist(playlistName, emptyList()))
                        return@thenAccept
                    }

                    val jsonTracks = it.getJSONArray("items")
                    val tracks = mutableListOf<SpotifyAudioTrack>()

                    for (jTrack in jsonTracks) {
                        val track = (jTrack as JSONObject).getJSONObject("track")
                        tracks.add(SpotifyAudioTrack.fromJson(track))
                    }

                    future.complete(SpotifyPlaylist(playlistName, tracks))
                }
                .exceptionally {
                    future.completeExceptionally(it)
                    return@exceptionally null
                }

        return future
    }

    fun search(title: String): CompletableFuture<SpotifyAudioTrack> {
        val request = Request.Builder()
                .url("https://api.spotify.com/v1/search?q=$title&type=track")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

        val future = CompletableFuture<SpotifyAudioTrack>()

        makeRequest(request)
                .thenAccept {
                    val results = it.getJSONObject("tracks").getJSONArray("items")

                    if (results.length() == 0) {
                        future.complete(null)
                        return@thenAccept
                    }

                    val track = results.getJSONObject(0)
                    future.complete(SpotifyAudioTrack.fromJson(track))
                }
                .exceptionally {
                    future.completeExceptionally(it)
                    return@exceptionally null
                }

        return future
    }

    private fun makeRequest(request: Request): CompletableFuture<JSONObject> {
        val fut = CompletableFuture<JSONObject>()

        JukeBot.httpClient.makeRequest(request).queue({
            val json = it.json()

            if (json == null) {
                fut.completeExceptionally(Exception("Expected JSON object, got null"))
                return@queue
            }

            fut.complete(json)
        }, {
            fut.completeExceptionally(it)
        })

        return fut
    }

    companion object {
        val PLAYLIST_PATTERN = Pattern.compile("^https?://(?:.*\\.)?spotify\\.com/(?:user/[a-zA-Z0-9_]+/)?playlist/([a-zA-Z0-9]+).*")!!
    }
}
