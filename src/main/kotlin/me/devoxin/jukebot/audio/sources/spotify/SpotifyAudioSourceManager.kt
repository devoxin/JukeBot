package me.devoxin.jukebot.audio.sources.spotify

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import me.devoxin.jukebot.audio.sources.spotify.loaders.*
import org.apache.http.HttpStatus
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.StringEntity
import org.slf4j.LoggerFactory
import java.io.DataInput
import java.io.DataOutput
import java.util.*

class SpotifyAudioSourceManager(private val clientId: String, private val clientSecret: String) : AudioSourceManager {
    private val httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager()!!

    private val lock = Object()
    private var disabled = false
    private var accessToken: String? = null
    private var tokenExpiration: Long = 0

    private val accessTokenExpired: Boolean
        get() = accessToken == null || System.currentTimeMillis() > (tokenExpiration - 5000)


    init {
        refreshAccessToken()
    }

    override fun getSourceName() = "spotify"

    override fun isTrackEncodable(track: AudioTrack) = false

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack {
        val isrc = DataFormatTools.readNullableText(input)
        val artwork = DataFormatTools.readNullableText(input)
        return SpotifyAudioTrack(this, trackInfo, isrc, artwork)
    }

    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        DataFormatTools.writeNullableText(output, (track as SpotifyAudioTrack).isrc)
        DataFormatTools.writeNullableText(output, track.artworkUrl)
    }

    override fun shutdown() {
        httpInterfaceManager.close()
    }

    override fun loadItem(manager: AudioPlayerManager, reference: AudioReference): AudioItem? {
        if (disabled) {
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

    private fun refreshAccessToken() {
        if (clientId.isNotEmpty() && clientSecret.isNotEmpty()) {
            refreshAccessTokenWithClientToken()
        } else {
            refreshAccessTokenAnonymously()
        }
    }

    private fun refreshAccessTokenAnonymously() {
        request(HttpGet("https://open.spotify.com/get_access_token"), validateToken = false).use {
            if (it.statusLine.statusCode != HttpStatus.SC_OK) {
                return log.warn("received code ${it.statusLine.statusCode} from spotify while trying to update anonymous access token")
            }

            extractToken(JsonParser.`object`().from(it.entity.content), isAnonymous = true)
        }
    }

    private fun refreshAccessTokenWithClientToken() {
        val base64Auth = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())

        val request = HttpPost("https://accounts.spotify.com/api/token").apply {
            addHeader("Authorization", "Basic $base64Auth")
            addHeader("Content-Type", "application/x-www-form-urlencoded")
            entity = StringEntity("grant_type=client_credentials")
        }

        request(request, validateToken = false).use {
            if (it.statusLine.statusCode != HttpStatus.SC_OK) {
                return log.warn("received code ${it.statusLine.statusCode} from spotify while trying to update access token")
            }

            extractToken(JsonParser.`object`().from(it.entity.content), isAnonymous = false)
        }
    }

    private fun extractToken(json: JsonObject, isAnonymous: Boolean) {
        if (json.has("error") && json.getString("error").startsWith("invalid_")) {
            log.error("spotify api access disabled: (${json.getString("error")})")
            disabled = true
            accessToken = null
            return
        }

        val tokenKey = "accessToken".takeIf { isAnonymous } ?: "access_token"

        if (json.isNull(tokenKey)) {
            log.error("cannot update spotify access token as response does not contain a token")
            accessToken = null
            return
        }

        accessToken = json.getString(tokenKey)
        tokenExpiration = if (isAnonymous) json.getLong("accessTokenExpirationTimestampMs") else System.currentTimeMillis() + (json.getLong("expires_in") * 1000)
        log.info("${if (isAnonymous) "anonymous " else ""}access token successfully refreshed")
    }

    internal fun request(request: HttpUriRequest, validateToken: Boolean = true): CloseableHttpResponse {
        if (validateToken) {
            if (accessTokenExpired) {
                synchronized(lock) {
                    if (accessTokenExpired) {
                        refreshAccessToken()
                    }
                }
            }

            if (accessTokenExpired) {
                throw IllegalStateException("Unable to make API request (token missing)")
            }
        }

        if (validateToken) {
            request.addHeader("Authorization", "Bearer $accessToken")
        }

        return httpInterfaceManager.`interface`.use { it.execute(request) }
    }

    companion object {
        private val log = LoggerFactory.getLogger(SpotifyAudioSourceManager::class.java)
        internal const val TRACK_URL_FORMAT = "https://open.spotify.com/track/%s"

        private val loaders = listOf(
            SpotifySearchLoader(),
            SpotifyTrackLoader(),
            SpotifyAlbumLoader(),
            SpotifyPlaylistLoader(),
            SpotifyRecommendationsLoader()
        )
    }
}
