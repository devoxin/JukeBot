package jukebot.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import jukebot.Database
import jukebot.JukeBot
import jukebot.utils.Helpers
import jukebot.utils.Permissions
import jukebot.utils.toTitleCase
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.audio.AudioSendHandler

import java.util.HashSet
import java.util.LinkedList
import java.util.Random
import java.util.concurrent.TimeUnit

class AudioHandler(private val guildId: Long, val player: AudioPlayer) : AudioEventAdapter(), AudioSendHandler {

    private val permissions = Permissions()
    val bassBooster = BassBooster(player)

    private var lastFrame: AudioFrame? = null
    private val selector = Random()

    val queue = LinkedList<AudioTrack>()
    private val skips = HashSet<Long>()
    private var channelId: Long? = null
    private var shouldAnnounce = true

    private var repeat = RepeatMode.NONE
    var isShuffleEnabled = false
        private set

    var trackPacketLoss = 0
    var trackPackets = 0

    val isPlaying: Boolean
        get() = player.playingTrack != null

    var current: AudioTrack? = null

    val repeatString: String
        get() = repeat.toString().toTitleCase()

    init {
        player.addListener(this)
    }

    fun enqueue(track: AudioTrack, userID: Long, playNext: Boolean): Boolean { // boolean: shouldAnnounce
        track.userData = userID

        if (!player.startTrack(track, true)) {
            if (playNext) {
                queue.add(0, track)
            } else {
                queue.offer(track)
            }
            return true
        }

        return false
    }

    fun toggleShuffle(): Boolean {
        isShuffleEnabled = !isShuffleEnabled
        return isShuffleEnabled
    }

    fun setRepeat(mode: RepeatMode) {
        repeat = mode
    }

    fun voteSkip(userID: Long): Int {
        skips.add(userID)
        return skips.size
    }

    fun setChannel(channelId: Long?) {
        this.channelId = channelId
    }

    fun setShouldAnnounce(shouldAnnounce: Boolean) {
        this.shouldAnnounce = shouldAnnounce
    }

    fun playNext() {
        var nextTrack: AudioTrack? = null

        if (repeat == RepeatMode.ALL && current != null) {
            val r = current!!.makeClone()
            r.userData = current!!.userData
            queue.offer(r)
        }

        if (repeat == RepeatMode.SINGLE && current != null) {
            nextTrack = current!!.makeClone()
            nextTrack!!.userData = current!!.userData
        } else if (!queue.isEmpty()) {
            nextTrack = if (isShuffleEnabled) queue.removeAt(selector.nextInt(queue.size)) else queue.poll()
        }

        if (nextTrack != null) {
            player.startTrack(nextTrack, false)
        } else {
            current = null
            player.stopTrack()
            bassBooster.boost(0.0f)

            val guild = JukeBot.shardManager.getGuildById(guildId)

            if (guild == null) { // Bot was kicked or something
                JukeBot.removePlayer(guildId)
                return
            }

            val audioManager = guild.audioManager

            if (audioManager.isConnected || audioManager.isAttemptingToConnect) {
                Helpers.schedule({ audioManager.closeAudioConnection() }, 1, TimeUnit.SECONDS)

                announce("Queue Concluded!",
                        "[Support the development of JukeBot!](https://www.patreon.com/Devoxin)\nSuggest features with the `feedback` command!")
            }
        }
    }

    private fun announce(title: String, description: String) {
        if (!shouldAnnounce || channelId == null) {
            return
        }

        val channel = JukeBot.shardManager.getTextChannelById(channelId!!)

        if (channel == null || !permissions.canSendTo(channel)) {
            return
        }

        channel.sendMessage(EmbedBuilder()
                .setColor(Database.getColour(channel.guild.idLong))
                .setTitle(title)
                .setDescription(Helpers.truncate(description, 1000))
                .build()
        ).queue(null, { err -> JukeBot.LOG.error("Encountered an error while posting track announcement", err) })
    }


    fun cleanup() {
        queue.clear()
        skips.clear()
        player.destroy()

        JukeBot.shardManager.getGuildById(guildId)?.audioManager?.sendingHandler = null
    }

    /*
     * Player Events
     */

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        skips.clear()
        trackPacketLoss = 0
        trackPackets = 0

        if (endReason.mayStartNext) {
            playNext()
        }
    }

    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        player.isPaused = false

        if (current == null || current!!.identifier == track.identifier) {
            announce("Now Playing", track.info.title)
        }
    }

    override fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
        if (repeat != RepeatMode.NONE)
            repeat = RepeatMode.NONE

        announce("Playback Error", "Playback of **${track.info.title}** encountered an error!\n" +
                exception.localizedMessage)
    }

    override fun onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long) {
        if (repeat != RepeatMode.NONE)
            repeat = RepeatMode.NONE

        announce("Track Stuck", "Playback of **${track.info.title}** has frozen and is unable to resume!")
        playNext()
    }

    /*
     * Packet Sending Events
     */

    override fun canProvide(): Boolean {
        lastFrame = player.provide()

        if (!player.isPaused) {
            if (lastFrame == null) {
                trackPacketLoss++
            } else {
                trackPackets++
            }
        }

        return lastFrame != null
    }

    override fun provide20MsAudio(): ByteArray {
        return lastFrame!!.data
    }

    override fun isOpus(): Boolean {
        return true
    }

    enum class RepeatMode {
        SINGLE, ALL, NONE
    }

}
