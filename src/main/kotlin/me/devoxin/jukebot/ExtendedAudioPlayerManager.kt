package me.devoxin.jukebot

import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonWriter
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.FunctionalResultHandler
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingNanoIpRoutePlanner
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.IpBlock
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block
import kotlinx.coroutines.future.await
import me.devoxin.flight.api.context.Context
import me.devoxin.jukebot.Launcher.config
import me.devoxin.jukebot.audio.AudioHandler
import me.devoxin.jukebot.audio.LoadResultHandler
import me.devoxin.jukebot.audio.sources.caching.CachingSourceManager
import me.devoxin.jukebot.audio.sources.deezer.DeezerAudioSourceManager
import me.devoxin.jukebot.audio.sources.delegate.*
import me.devoxin.jukebot.audio.sources.mixcloud.MixcloudAudioSourceManager
import me.devoxin.jukebot.audio.sources.pornhub.PornHubAudioSourceManager
import me.devoxin.jukebot.audio.sources.spotify.SpotifyAudioSourceManager
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class ExtendedAudioPlayerManager(val dapm: DefaultAudioPlayerManager = DefaultAudioPlayerManager(),
                                 disableYoutube: Boolean,
                                 disableYoutubeDelegate: Boolean,
                                 youtubeDelegationOnly: Boolean,
                                 disableHttp: Boolean,
                                 val enableNsfw: Boolean,
                                 val allowOpusEncoderConfiguration: Boolean) : AudioPlayerManager by dapm {
    val players = ConcurrentHashMap<Long, AudioHandler>()
    val delegateSource: DelegateHandler

    init {
        dapm.apply {
            configuration.setFrameBufferFactory(::NonAllocatingAudioFrameBuffer)
            configuration.setFilterHotSwapEnabled(true)

            if (Launcher.isSelfHosted || Launcher.shardManager.botId == 314145804807962634L) {
                configuration.resamplingQuality = AudioConfiguration.ResamplingQuality.HIGHEST
            } else {
                configuration.resamplingQuality = AudioConfiguration.ResamplingQuality.MEDIUM
            }

            registerSourceManager(CachingSourceManager())

            val deezerKey = config.opt("deezer_key")

            if (!deezerKey.isNullOrEmpty()) {
                registerSourceManager(DeezerAudioSourceManager(config["deezer_key"], config["deezer_arl", ""]))
            }

            registerSourceManager(MixcloudAudioSourceManager())

            if (enableNsfw) {
                registerSourceManager(PornHubAudioSourceManager())
            }

            registerSourceManager(SpotifyAudioSourceManager(config["spotify_client"], config["spotify_secret"]))

            val youtubeAudioSourceManager = YoutubeAudioSourceManager()

            if (!disableYoutube) {
                if (!config.ipv6Block.isNullOrEmpty()) {
                    log.info("using ipv6 block with RotatingNanoIpRoutePlanner")
                    val blocks = listOf<IpBlock<*>>(Ipv6Block(config.ipv6Block))
                    val planner = RotatingNanoIpRoutePlanner(blocks)
                    YoutubeIpRotatorSetup(planner).forSource(youtubeAudioSourceManager).setup()
                }

                if (!youtubeDelegationOnly) {
                    registerSourceManager(youtubeAudioSourceManager)
                }
            }

            registerSourceManager(SoundCloudAudioSourceManager.createDefault())
            registerSourceManager(BandcampAudioSourceManager())
            registerSourceManager(VimeoAudioSourceManager())
            registerSourceManager(TwitchStreamAudioSourceManager())
            registerSourceManager(GetyarnAudioSourceManager())

            if (!disableHttp) {
                val httpAudioSourceManager = HttpAudioSourceManager().apply {
                    configureBuilder {
                        val proxyHost = config.opt("proxy_host", null)
                        val proxyPort = config.getInt("proxy_port", 3128)

                        val proxyAuthUser = config.opt("proxy_user", null)
                        val proxyAuthPassword = config["proxy_password", ""]

                        if (!proxyHost.isNullOrEmpty()) {
                            it.setProxy(HttpHost(proxyHost, proxyPort))

                            if (!proxyAuthUser.isNullOrEmpty()) {
                                val credentialsProvider = BasicCredentialsProvider()
                                credentialsProvider.setCredentials(AuthScope(proxyHost, proxyPort), UsernamePasswordCredentials(proxyAuthUser, proxyAuthPassword))
                                it.setDefaultCredentialsProvider(credentialsProvider)
                            }
                        }
                    }
                }

                registerSourceManager(httpAudioSourceManager)
            }

            val delegates = mutableSetOf<DelegateSource>().apply {
                if (!disableYoutube && !disableYoutubeDelegate) {
                    add(YoutubeDelegateSource(this@ExtendedAudioPlayerManager, youtubeAudioSourceManager))
                }

                if (!deezerKey.isNullOrEmpty()) {
                    add(DeezerDelegateSource(this@ExtendedAudioPlayerManager, source(DeezerAudioSourceManager::class.java)))
                }

                add(SoundcloudDelegateSource(this@ExtendedAudioPlayerManager, source(SoundCloudAudioSourceManager::class.java)))
            }

            delegateSource = DelegateHandler(delegates)
        }
    }

    @Synchronized
    fun getOrCreatePlayer(guildId: Long, messageChannelId: Long, voiceChannelId: Long): AudioHandler {
        val guild = Launcher.shardManager.getGuildById(guildId)
            ?: throw IllegalStateException("Cannot create a player for a non-existent guild!")

        val player = players.computeIfAbsent(guildId) { AudioHandler(guildId, messageChannelId, voiceChannelId, createPlayer()) }
        val audioManager = guild.audioManager

        if (audioManager.sendingHandler == null) {
            audioManager.sendingHandler = player
        }

        return player
    }

    fun removePlayer(guildId: Long) {
        players.remove(guildId)?.cleanup()
    }

    fun toBase64String(track: AudioTrack): String {
        return ByteArrayOutputStream().use {
            encodeTrack(MessageOutput(it), track)
            Base64.getEncoder().encodeToString(it.toByteArray())
        }
    }

    fun toAudioTrack(encoded: String): AudioTrack {
        val b64 = Base64.getDecoder().decode(encoded)
        return ByteArrayInputStream(b64).use { decodeTrack(MessageInput(it)).decodedTrack }
    }

    fun toJsonString(playlist: AudioPlaylist): String {
        val selectedIndex = playlist.selectedTrack?.let(playlist.tracks::indexOf) ?: -1

        return JsonWriter.string()
            .`object`()
            .value("name", playlist.name)
            .value("search", playlist.isSearchResult)
            .value("selected", selectedIndex)
            .array("tracks", playlist.tracks.map(::toBase64String))
            .end()
            .done()
    }

    fun toPlaylist(encoded: String): BasicAudioPlaylist {
        val obj = JsonParser.`object`().from(encoded)

        val name = obj.getString("name")
        val tracks = obj.getArray("tracks").map { toAudioTrack(it as String) }
        val index = obj.getInt("selected")

        val selectedTrack = if (index > -1) tracks[index] else null
        val search = obj.getBoolean("search")

        return BasicAudioPlaylist(name, tracks, selectedTrack, search)
    }

    fun loadIdentifier(identifier: String, ctx: Context, handler: AudioHandler, useSelection: Boolean, playNext: Boolean = false) {
        loadItem(identifier, LoadResultHandler(ctx, identifier, handler, useSelection, playNext))
    }

    suspend fun loadAsync(identifier: String): AudioItem? {
        val future = CompletableFuture<AudioItem?>()
        loadItem(identifier, FunctionalResultHandler(
            future::complete,
            future::complete,
            { future.complete(null) },
            future::completeExceptionally
        ))
        return future.await()
    }

    companion object {
        private val log = LoggerFactory.getLogger(ExtendedAudioPlayerManager::class.java)
    }
}
