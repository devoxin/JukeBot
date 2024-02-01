package me.devoxin.jukebot.audio

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameFlags
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import io.sentry.Sentry
import io.sentry.event.BreadcrumbBuilder
import io.sentry.event.Event
import io.sentry.event.EventBuilder
import io.sentry.event.interfaces.ExceptionInterface
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.devoxin.flight.internal.utils.TextUtils
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.Launcher
import me.devoxin.jukebot.audio.AudioHandler.RepeatMode.ALL
import me.devoxin.jukebot.audio.AudioHandler.RepeatMode.SINGLE
import me.devoxin.jukebot.audio.sources.spotify.SpotifyAudioTrack
import me.devoxin.jukebot.extensions.await
import me.devoxin.jukebot.extensions.capitalise
import me.devoxin.jukebot.extensions.toTimeString
import me.devoxin.jukebot.extensions.truncate
import me.devoxin.jukebot.utils.Helpers
import me.devoxin.jukebot.utils.Scopes
import me.devoxin.jukebot.utils.collections.FixedDeque
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.Permission.*
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.TimeUnit

class AudioHandler(private val guildId: Long,
                   var channelId: Long,
                   private val initialVoiceChannelId: Long,
                   val player: AudioPlayer) : AudioEventAdapter(), AudioSendHandler {
    private val mutex = Mutex()

    private val mutableFrame = MutableAudioFrame()
    private val buffer = ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize())

    private var initialConnect = false

    // Playback Settings
    val bassBooster = BassBooster(player)
    var repeat = RepeatMode.NONE
    var shuffle = false

    val queue = LinkedList<AudioTrack>()
    private val skips = hashSetOf<Long>()

    var lastAnnouncement: Message? = null
    var shouldAnnounce = true

    // Performance Tracking
    var trackPacketLost = 0
        private set
    var trackPacketsSent = 0
        private set

    // Player Stuff
    private val autoPlay = AutoPlay(guildId)

    private val history = FixedDeque<AudioTrack>(10)
    private var backwards = false
    val canGoBack: Boolean get() = history.isNotEmpty()

    val isPlaying: Boolean get() = player.playingTrack != null
    val isEncoding: Boolean get() = AudioFrameFlags.OPUS_PASSTHROUGH !in mutableFrame.flags

    private val guild: Guild?
        get() = Launcher.shardManager.getGuildById(guildId)

    init {
        player.addListener(this)
        mutableFrame.setBuffer(buffer)
    }

    /**
     * Enqueues a track to be played.
     * @param track
     *        The track to add to the queue, or to begin playing.
     * @param userId
     *        The ID of the user who requested the track.
     *
     * @return true, if the calling function should announce that the track has been enqueued.
     */
    fun enqueue(track: AudioTrack, userId: Long, playNext: Boolean): Boolean {
        if (!initialConnect) {
            val guild = guild
            val channel = guild?.getVoiceChannelById(initialVoiceChannelId)

            if (guild == null || channel == null) {
                Launcher.playerManager.removePlayer(guildId)
                return false
            }

            guild.audioManager.openAudioConnection(channel)
            initialConnect = true
        }

        track.userData = userId

        if (!player.startTrack(track, true)) {
            if (playNext) {
                queue.add(0, track)
            } else {
                queue.add(track)
            }
            return true
        }

        return false
    }

    fun voteSkip(userID: Long): Int {
        skips.add(userID)
        return skips.size
    }

    fun previous() {
        if (!canGoBack) {
            return
        }

        val track = player.playingTrack
        backwards = true

        if (track != null) {
            queue.add(0, track.makeClone())
        }

        player.playTrack(history.removeLast().makeClone())
    }

    fun next(shouldAutoPlay: Boolean = true, lastTrack: AudioTrack? = player.playingTrack) {
        var nextTrack: AudioTrack? = null

        if (lastTrack is SpotifyAudioTrack && lastTrack.position >= minOf(lastTrack.duration * 0.20, 30000.0)) {
            autoPlay.store(lastTrack)
        }

        if (lastTrack != null && repeat != RepeatMode.NONE) {
            val cloned = lastTrack.makeClone()

            when (repeat) {
                ALL -> queue.offer(cloned)
                SINGLE -> nextTrack = cloned
                else -> {}
            }
        }

        if (nextTrack == null && !queue.isEmpty()) {
            nextTrack = if (shuffle) queue.removeAt(selector.nextInt(queue.size)) else queue.poll()
        }

        if (nextTrack != null) {
            return player.playTrack(nextTrack)
        }

        if (shouldAutoPlay && autoPlay.enabled && autoPlay.isUsable) {
            val recommendedTrack = autoPlay.getRelatedTrack()

            if (recommendedTrack != null) {
                return player.playTrack(recommendedTrack)
            }
            
            announce("AutoPlay", "AutoPlay was unable to find a track to play.", set = false)
        }

        announce("Queue Concluded", "Keep the party going by adding more tracks!", set = false)
        player.stopTrack()
        bassBooster.boost(0)

        val audioManager = guild?.audioManager
            ?: return Launcher.playerManager.removePlayer(guildId).let { cleanup() }

        if (audioManager.isConnected) {
            Helpers.schedule(audioManager::closeAudioConnection, 1, TimeUnit.SECONDS)
        }

        cleanup()
    }

    private fun announce(title: String?,
                         description: String,
                         thumbnailUrl: String? = null,
                         set: Boolean = true,
                         extra: MessageCreateBuilder.() -> Unit = {}) {
        if (!shouldAnnounce) {
            return
        }

        val channel = guild?.getChannelById(TextChannel::class.java, channelId)
            ?.takeIf { it.guild.selfMember.hasPermission(VIEW_CHANNEL, MESSAGE_SEND, MESSAGE_EMBED_LINKS) }
            ?: return

        Scopes.IO.launch {
            announce0(channel, title, description, thumbnailUrl, set, extra)
        }
    }

    private suspend fun announce0(channel: GuildMessageChannel,
                                  title: String?,
                                  description: String,
                                  thumbnailUrl: String?,
                                  set: Boolean,
                                  extra: MessageCreateBuilder.() -> Unit) {
        mutex.withLock {
            lastAnnouncement?.runCatching { delete().await() }

            channel.runCatching {
                sendMessage(MessageCreateBuilder()
                    .addEmbeds(EmbedBuilder()
                        .setColor(Database.getColour(guildId))
                        .setTitle(title)
                        .setDescription(description.truncate(1000))
                        .setThumbnail(thumbnailUrl)
                        .build())
                    .apply(extra)
                    .build())
                    .await()
            }.onSuccess {
                if (set) lastAnnouncement = it
            }
        }
    }

    private fun setNick(nick: String?) {
        guild?.selfMember
            ?.takeIf { it.hasPermission(NICKNAME_CHANGE) && Database.getIsMusicNickEnabled(guildId) }
            ?.modifyNickname(nick)?.queue()
    }

    fun cleanup() {
        queue.clear()
        skips.clear()
        player.destroy()

        guild?.audioManager?.sendingHandler = null
        setNick(null)
    }

    /*
     *  +===================+
     *  |   Player Events   |
     *  +===================+
     */
    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        player.isPaused = false

        if (history.isNotEmpty() && track.identifier == history.first().identifier) {
            // TODO check if last sent message ID is the now playing message?
            return
        }

        val duration = track.takeIf { !it.info.isStream }?.duration?.toTimeString() ?: "∞"
        val requester = if (track.userData as Long == Launcher.shardManager.botId) "AutoPlay" else "<@${track.userData}>"

        announce(null, "**${track.info.title}**\n*${track.info.author} — $duration*\n$requester", (track as? SpotifyAudioTrack)?.artworkUrl) {
            setComponents(
                ActionRow.of(
                    Button.secondary("prev:$guild", Emoji.fromCustom("prev", 1200984412611948605, false)),
                    Button.secondary("play:$guild", Emoji.fromCustom("pause", 1200984439958798458, false)),
                    Button.secondary("next:$guild", Emoji.fromCustom("next", 1200984449068843099, false))
                )
            )
        }

        val nick = TextUtils.truncate("${track.info.title} - ${track.info.author}", 32)
        setNick(nick)
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        skips.clear()
        trackPacketLost = 0
        trackPacketsSent = 0

        if (!backwards) {
            history.add(track)
        }

        backwards = false

        if (endReason.mayStartNext) {
            next(lastTrack = track)
        }
    }

    override fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
        val breadCrumb = BreadcrumbBuilder()
            .setCategory("AudioHandler")
            .setMessage("Track ID: ${track.identifier}")
            .build()

        val eventBuilder = EventBuilder()
            .withMessage(exception.message)
            .withLevel(Event.Level.ERROR)
            .withSentryInterface(ExceptionInterface(exception))
            .withBreadcrumbs(listOf(breadCrumb))

        Sentry.capture(eventBuilder)
        repeat = RepeatMode.NONE

        announce("Track Unavailable", "An error occurred during the playback of **${track.info.title}**\nSkipping...", set = false)
    }

    override fun onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long) {
        repeat = RepeatMode.NONE

        announce("Track Unavailable", "The track **${track.info.title}** has frozen and cannot be played\nSkipping...", set = false)
        next(lastTrack = track)
    }

    /*
     *  +=======================+
     *  |   JDA Audio Sending   |
     *  +=======================+
     */
    override fun canProvide(): Boolean {
        val frameProvided = player.provide(mutableFrame)

        if (!player.isPaused) {
            if (!frameProvided) {
                trackPacketLost++
            } else {
                trackPacketsSent++
            }
        }

        return frameProvided
    }

    override fun provide20MsAudio(): ByteBuffer = buffer.flip()
    override fun isOpus() = true

    enum class RepeatMode {
        ALL,
        SINGLE,
        NONE;

        fun humanized(): String {
            return this.toString().lowercase().capitalise()
        }
    }

    companion object {
        const val EXPECTED_PACKET_COUNT_PER_MIN = ((60 * 1000) / 20).toDouble()
        private val selector = Random()
    }
}
