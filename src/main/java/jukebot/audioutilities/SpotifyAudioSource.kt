package jukebot.audioutilities

import jukebot.JukeBot
import jukebot.utils.Helpers
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class SpotifyAudioSource(private val clientId: String, private val clientSecret: String) {

    private val httpClient: OkHttpClient = OkHttpClient()
    private var accessToken: String = ""

    init {
        refreshAccessToken()
    }

    fun isEnabled(): Boolean {
        return clientId.isNotBlank() && clientSecret.isNotBlank() && accessToken.isNotBlank()
    }

    private fun refreshAccessToken() {
        val base64Auth = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())
        val body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), "grant_type=client_credentials")

        val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .addHeader("Authorization", "Basic $base64Auth")
                .post(body)
                .build()

        httpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                JukeBot.LOG.error("[SpotifyAudioSource] Unable to update Spotify access token!", e)
                Helpers.schedule({ refreshAccessToken() }, 5, TimeUnit.MINUTES)
            }

            override fun onResponse(call: Call, response: Response) {
                val resBody = response.body()

                if (resBody == null) {
                    JukeBot.LOG.error("[SpotifyAudioSource] Response body was null!")
                    return
                }

                val json = JSONObject(resBody.string())

                if (json.has("error") && json.getString("error").startsWith("invalid_")) {
                    JukeBot.LOG.error("[SpotifyAudioSource] Spotify API access disabled (${json.getString("error")})")
                    return
                }

                val refreshIn = json.getInt("expires_in")

                accessToken = json.getString("access_token")
                Helpers.schedule({ refreshAccessToken() }, (refreshIn * 1000) - 10000, TimeUnit.MILLISECONDS)

                JukeBot.LOG.info("[SpotifyAudioSource] Updated access token to $accessToken")
            }
        })
    }

    fun getTracksFromPlaylist(userId: String, playlistId: String, callback: (List<SparseSpotifyAudioTrack>) -> Unit) {
        val request = Request.Builder()
                .url("https://api.spotify.com/v1/users/$userId/playlists/$playlistId/tracks")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

        httpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                val resBody = response.body() ?: return callback(emptyList())

                val list: MutableList<SparseSpotifyAudioTrack> = ArrayList()
                val json = JSONObject(resBody.string())

                if (!json.has("items")) {
                    return callback(emptyList())
                }

                val tracks = json.getJSONArray("items")

                for (i in 0..json.length()) {
                    val t = tracks.getJSONObject(i).getJSONObject("track")
                    val artist = t.getJSONArray("artists").getJSONObject(0).getString("name")
                    val trackName = t.getString("name")
                    list.add(SparseSpotifyAudioTrack(trackName, artist))
                }

                callback(list)
            }
        })
    }

}
