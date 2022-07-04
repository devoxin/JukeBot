package jukebot.audio.sourcemanagers.spotify

import com.grack.nanojson.JsonParser
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import jukebot.JukeBot
import jukebot.audio.sourcemanagers.spotify.loaders.SpotifyAlbumLoader
import jukebot.audio.sourcemanagers.spotify.loaders.SpotifyPlaylistLoader
import jukebot.audio.sourcemanagers.spotify.loaders.SpotifyTrackLoader
import jukebot.utils.Helpers
import org.apache.http.HttpStatus
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.StringEntity
import org.slf4j.LoggerFactory
import java.io.DataInput
import java.io.DataOutput
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SpotifyAudioSourceManager(
    private val clientId: String,
    private val clientSecret: String
) : AudioSourceManager {
    private val trackLoaderPool = Executors.newFixedThreadPool(10)

    private val httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager()!!
    internal var accessToken: String = ""
        private set

    init {
        if (clientId.isNotEmpty() && clientSecret.isNotEmpty()) {
            refreshAccessToken()
        }
    }

    /**
     * Source manager shizzle
     */
    override fun getSourceName() = "spotify"

    override fun isTrackEncodable(track: AudioTrack) = false

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack {
        throw UnsupportedOperationException("no")
    }

    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        throw UnsupportedOperationException("no")
    }

    override fun shutdown() {
        httpInterfaceManager.close()
    }

    override fun loadItem(manager: AudioPlayerManager, reference: AudioReference): AudioItem? {
        if (accessToken.isEmpty()) {
            return null
        }

        return try {
            loadItemOnce(reference.identifier)
        } catch (exception: FriendlyException) {
            // In case of a connection reset exception, try once more.
            if (HttpClientTools.isRetriableNetworkException(exception.cause)) {
                loadItemOnce(reference.identifier)
            } else {
                throw exception
            }
        }
    }

    private fun loadItemOnce(identifier: String): AudioItem? {
        for (loader in loaders) {
            val matcher = loader.pattern.matcher(identifier)

            if (matcher.find()) {
                return loader.load(this, matcher)
            }
        }

        return null
    }

    internal fun doYoutubeSearch(identifier: String): AudioItem? {
        val ytasm = JukeBot.playerManager.dapm.source(YoutubeAudioSourceManager::class.java)
        return ytasm.loadItem(JukeBot.playerManager.dapm, AudioReference(identifier, null))
    }

    internal fun queueYoutubeSearch(identifier: String): CompletableFuture<AudioItem?> {
        val future = CompletableFuture<AudioItem?>()

        trackLoaderPool.submit {
            try {
                future.complete(doYoutubeSearch(identifier))
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }

        return future
    }

    /**
     * Spotify shizzle
     */
    private fun refreshAccessToken() {
        val base64Auth = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())

        request(HttpPost.METHOD_NAME, "https://accounts.spotify.com/api/token") {
            addHeader("Authorization", "Basic $base64Auth")
            addHeader("Content-Type", "application/x-www-form-urlencoded")
            entity = StringEntity("grant_type=client_credentials")
        }.use {
            if (it.statusLine.statusCode != HttpStatus.SC_OK) {
                logger.warn("Received code ${it.statusLine.statusCode} from Spotify while trying to update access token!")
                Helpers.schedule(::refreshAccessToken, 1, TimeUnit.MINUTES)
                return
            }

            val json = JsonParser.`object`().from(it.entity.content)

            if (json.has("error") && json.getString("error").startsWith("invalid_")) {
                logger.error("Spotify API access disabled (${json.getString("error")})")
                accessToken = ""
                return
            }

            val refreshIn = json.getInt("expires_in")
            accessToken = json.getString("access_token")
            Helpers.schedule(::refreshAccessToken, (refreshIn * 1000) - 10000, TimeUnit.MILLISECONDS)

            logger.info("Access token successfully refreshed")
        }
    }

    /**
     * Utils boiiii
     */
    internal fun request(url: String, requestBuilder: RequestBuilder.() -> Unit): CloseableHttpResponse {
        return request(HttpGet.METHOD_NAME, url, requestBuilder)
    }

    private fun request(method: String, url: String, requestBuilder: RequestBuilder.() -> Unit): CloseableHttpResponse {
        return httpInterfaceManager.`interface`.use {
            it.execute(RequestBuilder.create(method).setUri(url).apply(requestBuilder).build())
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SpotifyAudioSourceManager::class.java)

        private val loaders = listOf(
            SpotifyTrackLoader(),
            SpotifyAlbumLoader(),
            SpotifyPlaylistLoader()
        )
    }
}
