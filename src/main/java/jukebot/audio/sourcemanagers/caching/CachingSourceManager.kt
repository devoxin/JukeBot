package jukebot.audio.sourcemanagers.caching

//import com.sedmelluq.discord.lavaplayer.source.youtube.CacheProvider
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.*
import jukebot.JukeBot
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.exceptions.JedisConnectionException
import redis.clients.jedis.params.SetParams
import java.io.DataInput
import java.io.DataOutput
import java.util.concurrent.TimeUnit

class CachingSourceManager : AudioSourceManager/*, CacheProvider*/ {

    init {
        try {
            jedisPool.resource.use {
                JukeBot.LOG.info("Connected to Redis; caching available.")
            }
        } catch (e: JedisConnectionException) {
            JukeBot.LOG.warn("Unable to connect to Redis; caching unavailable!")
            jedisPool.close()
            enabled = false
        }
    }

    override fun getSourceName() = "caching"

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack {
        throw UnsupportedOperationException("This source manager does not support the decoding of tracks.")
    }

    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        throw UnsupportedOperationException("This source manager does not support the encoding of tracks.")
    }

    override fun isTrackEncodable(track: AudioTrack) = false

    override fun loadItem(manager: DefaultAudioPlayerManager, reference: AudioReference): AudioItem? {
        return null
//        if (jedisPool.isClosed) {
//            return null
//        }
//
//        totalHits++
//
//        if (reference.identifier.startsWith("s!")) {
//            if (reference.identifier.split("!")[2].toInt() < 2) {
//                return null
//            }
//        }
//
//        val identifier = if (reference.identifier.startsWith("s!")) {
//            reference.identifier.split("!")[1]
//        } else {
//            reference.identifier
//        }
//
//        jedisPool.resource.use {
//            val encoded = it.get(identifier)
//                ?: return null
//
//            successfulHits++
//
//            if (encoded.startsWith('{')) {
//                return JukeBot.playerManager.toPlaylist(encoded)
//            }
//
//            return JukeBot.playerManager.toAudioTrack(encoded)
//        }
    }

//    override fun cacheVideoFormat(identifier: String, format: YoutubeAudioTrack.FormatWithUrl, ttl: Long) {
//    }
//
//    override fun removeVideoFormat(identifier: String) {
//    }
//
//    override fun getVideoFormat(identifier: String): YoutubeAudioTrack.FormatWithUrl? {
//        return null
//    }
//
//    override fun cacheUnavailableVideo(identifier: String, unavailableReason: String) {
//        if (jedisPool.isClosed) {
//            return
//        }
//
//        jedisPool.resource.use {
//            it.set(identifier, unavailableReason)
//        }
//    }
//
//    override fun checkUnavailable(identifier: String): String? {
//        if (jedisPool.isClosed) {
//            return null
//        }
//
//        return jedisPool.resource.use {
//            it.get(identifier)
//        }
//    }

    override fun shutdown() {
        jedisPool.close()
    }

    companion object {
        var enabled = true
            private set

        var totalHits = 0
        var successfulHits = 0

        private val jedisPool = JedisPool(JedisPoolConfig(), "localhost")

        private val PLAYLIST_TTL = TimeUnit.HOURS.toMillis(2)
        private val SEARCH_TTL = TimeUnit.HOURS.toMillis(12)
        private val TRACK_TTL = TimeUnit.HOURS.toMillis(12)

        fun cache(identifier: String, item: AudioItem) {
            if (jedisPool.isClosed) {
                return
            }

            if (item is AudioTrack) {
                jedisPool.resource.use {
                    val encoded = JukeBot.playerManager.toBase64String(item)
                    val setParams = SetParams.setParams().nx().px(TRACK_TTL)
                    it.set(identifier, encoded, setParams)
                }
            } else if (item is AudioPlaylist) {
                jedisPool.resource.use {
                    val ttl = if (item.isSearchResult) SEARCH_TTL else PLAYLIST_TTL
                    val encoded = JukeBot.playerManager.toJsonString(item)
                    val setParams = SetParams.setParams().nx().px(ttl)
                    it.set(identifier, encoded, setParams)
                }
            }
        }
    }

}
