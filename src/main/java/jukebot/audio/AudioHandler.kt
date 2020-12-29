package jukebot.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import io.sentry.Sentry
import io.sentry.event.BreadcrumbBuilder
import io.sentry.event.Event
import io.sentry.event.EventBuilder
import io.sentry.event.interfaces.ExceptionInterface
import jukebot.Database
import jukebot.JukeBot
import jukebot.utils.Helpers
import jukebot.utils.toTimeString
import jukebot.utils.toTitleCase
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.TimeUnit

class AudioHandler(private val guildId: Long, val player: AudioPlayer) : AudioEventAdapter(), AudioSendHandler {
    private val mutableFrame = MutableAudioFrame()
    private val buffer = ByteBuffer.allocate(1024)
    // ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize())

    // Playback Settings
    val bassBooster = BassBooster(player)
    var repeat = RepeatMode.NONE
    var shuffle = false

    val queue = LinkedList<AudioTrack>()
    private val skips = hashSetOf<Long>()
    private val selector = Random()

    var shouldAnnounce = true
    var channelId: Long? = null

    // Performance Tracking
    var trackPacketLost = 0
    var trackPacketsSent = 0

    // Player Stuff
    val autoPlay = AutoPlay(guildId)
    var previous: AudioTrack? = null
    var current: AudioTrack? = null
    val isPlaying: Boolean
        get() = player.playingTrack != null

    init {
        player.addListener(this)
        this.mutableFrame.setBuffer(buffer)
    }

    fun enqueue(track: AudioTrack, userID: Long, playNext: Boolean): Boolean { // boolean: shouldAnnounce
        track.userData = userID

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

    fun playNext(shouldAutoPlay: Boolean = true) {
        var nextTrack: AudioTrack? = null

        current?.let {
            if (it.sourceManager.sourceName == "youtube") {
                autoPlay.store(it.info.title)
            }
        }

        if (current != null && repeat != RepeatMode.NONE) {
            val cloned = current!!.makeClone().also { c -> c.userData = current!!.userData }

            if (repeat == RepeatMode.ALL) {
                queue.offer(cloned)
            } else if (repeat == RepeatMode.SINGLE) {
                nextTrack = cloned
            }
        }

        if (nextTrack == null && !queue.isEmpty()) {
            nextTrack = if (shuffle) queue.removeAt(selector.nextInt(queue.size)) else queue.poll()
        }

        if (nextTrack != null) {
            return player.playTrack(nextTrack)
        }

        if (shouldAutoPlay && autoPlay.enabled && autoPlay.hasSufficientData) {
            autoPlay.getRelatedTrack()
                .thenAccept(player::playTrack)
                .exceptionally {
                    playNext(false)
                    announce("AutoPlay", "AutoPlay encountered an error.\nWe're sorry for any inconvenience caused!")
                    Sentry.capture(it)
                    return@exceptionally null
                }
            return
        }

        current = null
        player.stopTrack()
        bassBooster.boost(0.0f)

        val audioManager = JukeBot.shardManager.getGuildById(guildId)?.audioManager
            ?: return JukeBot.removePlayer(guildId)

        if (audioManager.isConnected) {
            Helpers.schedule(audioManager::closeAudioConnection, 1, TimeUnit.SECONDS)

            if (Database.getIsPremiumServer(guildId)) {
                announce("Queue Concluded", "Enable AutoPlay to keep the party going!")
            } else {
                announce("Queue Concluded!",
                    "[Support the development of JukeBot!](https://www.patreon.com/Devoxin)")
            }

            setNick(null)
        }
    }

    private fun announce(title: String, description: String) {
        if (!shouldAnnounce || channelId == null) {
            return
        }

        val channel = JukeBot.shardManager.getTextChannelById(channelId!!)
            ?.takeIf { Helpers.canSendTo(it) }
            ?: return

        channel.sendMessage(EmbedBuilder()
            .setColor(Database.getColour(channel.guild.idLong))
            .setTitle(title)
            .setDescription(Helpers.truncate(description, 1000))
            .build()
        ).queue()
    }

    private fun setNick(nick: String?) {
        val guild = JukeBot.shardManager.getGuildById(guildId) ?: return

        if (guild.selfMember.hasPermission(Permission.NICKNAME_CHANGE) && Database.getIsMusicNickEnabled(guild.idLong)) {
            guild.selfMember.modifyNickname(nick).queue()
        }
    }

    fun cleanup() {
        queue.clear()
        skips.clear()
        player.destroy()

        JukeBot.shardManager.getGuildById(guildId)?.audioManager?.sendingHandler = null
    }

    /*
     *  +===================+
     *  |   Player Events   |
     *  +===================+
     */
    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        player.isPaused = false

        if (current == null || current!!.identifier != track.identifier) {
            current = track
            val durString = if (track.info.isStream) "LIVE" else track.info.length.toTimeString()
            announce("Now Playing", "${track.info.title} - `$durString`")

            val title = track.info.title
            val nick = if (title.length > 32) "${title.substring(0, 29)}..." else title
            setNick(nick)
        }
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        skips.clear()
        trackPacketLost = 0
        trackPacketsSent = 0

        previous = track

        if (endReason.mayStartNext) {
            playNext()
        }
    }

    override fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
        val breadCrumb = BreadcrumbBuilder()
            .setCategory("AudioHandler")
            .setMessage("Track ID: ${track.identifier}")
            .build()

        val eventBuilder = EventBuilder().withMessage(exception.message)
            .withLevel(Event.Level.ERROR)
            .withSentryInterface(ExceptionInterface(exception))
            .withBreadcrumbs(listOf(breadCrumb))

        Sentry.capture(eventBuilder)

        if (repeat != RepeatMode.NONE)
            repeat = RepeatMode.NONE

        val problem = Helpers.rootCauseOf(exception)

//        if (banned && Database.getIsAutoPlayEnabled(guildId)) {
//            Database.setAutoPlayEnabled(guildId, false)
//        }

        announce("Playback Error", "Playback of **${track.info.title}** encountered an error!\n" +
            problem.localizedMessage)
    }

    override fun onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long) {
        if (repeat != RepeatMode.NONE)
            repeat = RepeatMode.NONE

        announce("Track Stuck", "Playback of **${track.info.title}** has frozen and is unable to resume!")
        playNext()
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

    override fun provide20MsAudio(): ByteBuffer {
        return buffer.flip()
    }

    override fun isOpus(): Boolean {
        return true
    }

    enum class RepeatMode {
        ALL,
        SINGLE,
        NONE;

        fun humanized(): String {
            return this.toString().toLowerCase().toTitleCase()
        }
    }

    companion object {
        const val EXPECTED_PACKET_COUNT_PER_MIN = ((60 * 1000) / 20).toDouble()
    }
}
