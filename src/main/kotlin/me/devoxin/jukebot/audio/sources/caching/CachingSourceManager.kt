package me.devoxin.jukebot.audio.sources.caching

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.*
import me.devoxin.jukebot.Launcher
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.exceptions.JedisConnectionException
import redis.clients.jedis.params.SetParams
import java.io.DataInput
import java.io.DataOutput
import java.util.concurrent.TimeUnit

class CachingSourceManager : AudioSourceManager {
    init {
        try {
            jedisPool.resource.use {
                log.info("redis caching available")
            }
        } catch (e: JedisConnectionException) {
            log.warn("redis caching unavailable")
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

    override fun loadItem(manager: AudioPlayerManager, reference: AudioReference): AudioItem? {
        if (jedisPool.isClosed) {
            return null
        }

        totalHits++

        jedisPool.resource.use {
            val encoded = it.get(reference.identifier)
                ?: return null

            successfulHits++

            if (encoded.startsWith('{')) {
                return Launcher.playerManager.toPlaylist(encoded)
            }

            return Launcher.playerManager.toAudioTrack(encoded)
        }
    }

    override fun shutdown() {
        jedisPool.close()
    }

    companion object {
        private val log = LoggerFactory.getLogger(CachingSourceManager::class.java)
        private val jedisPool = JedisPool(JedisPoolConfig(), "redis://localhost:6379/")

        private val PLAYLIST_TTL = TimeUnit.HOURS.toMillis(2)
        private val SEARCH_TTL = TimeUnit.HOURS.toMillis(12)
        private val TRACK_TTL = TimeUnit.HOURS.toMillis(12)

        var enabled = true
            private set

        var totalHits = 0
        var successfulHits = 0

        fun cache(identifier: String, item: AudioItem) {
            if (jedisPool.isClosed) {
                return
            }

            if (item is AudioTrack) {
                jedisPool.resource.use {
                    val encoded = Launcher.playerManager.toBase64String(item)
                    val setParams = SetParams.setParams().nx().px(TRACK_TTL)
                    it.set(identifier, encoded, setParams)
                }
            } else if (item is AudioPlaylist) {
                jedisPool.resource.use {
                    val ttl = if (item.isSearchResult) SEARCH_TTL else PLAYLIST_TTL
                    val encoded = Launcher.playerManager.toJsonString(item)
                    val setParams = SetParams.setParams().nx().px(ttl)
                    it.set(identifier, encoded, setParams)
                }
            }
        }
    }
}
