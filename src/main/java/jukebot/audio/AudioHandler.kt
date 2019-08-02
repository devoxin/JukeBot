package jukebot.audio

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
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
                queue.offer(track)
            }
            return true
        }

        return false
    }

    fun voteSkip(userID: Long): Int {
        skips.add(userID)
        return skips.size
    }

    fun playNext() {
        var nextTrack: AudioTrack? = null

        if (current != null) {
            if (repeat == RepeatMode.ALL) {
                    val r = current!!.makeClone()
                    r.userData = current!!.userData
                    queue.offer(r)
            } else if (repeat == RepeatMode.SINGLE) {
                nextTrack = current!!.makeClone()
                nextTrack.userData = current!!.userData
            }
        }

        if (nextTrack == null && !queue.isEmpty()) {
            nextTrack = if (shuffle) {
                queue.removeAt(selector.nextInt(queue.size))
            } else {
                queue.poll()
            }
        }

        if (nextTrack != null) {
            player.startTrack(nextTrack, false)
            return
        }

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
            Helpers.schedule(audioManager::closeAudioConnection, 1, TimeUnit.SECONDS)

            announce("Queue Concluded!",
                    "[Support the development of JukeBot!](https://www.patreon.com/Devoxin)\nSuggest features with the `feedback` command!")

            setNick(null)
        }
    }

    private fun announce(title: String, description: String) {
        if (!shouldAnnounce || channelId == null) {
            return
        }

        val channel = JukeBot.shardManager.getTextChannelById(channelId!!)

        if (channel == null || !Helpers.canSendTo(channel)) {
            return
        }

        channel.sendMessage(EmbedBuilder()
                .setColor(Database.getColour(channel.guild.idLong))
                .setTitle(title)
                .setDescription(Helpers.truncate(description, 1000))
                .build()
        ).queue(null, { err -> JukeBot.LOG.error("Encountered an error while posting track announcement", err) })
    }

    fun setNick(nick: String?) {
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
     * Player Events
     */

    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        player.isPaused = false

        if (current == null || current!!.identifier != track.identifier) {
            current = track
            announce("Now Playing", "${track.info.title} - `${track.info.length.toTimeString()}`")

            val title = track.info.title
            val nick = if (title.length > 32) "${title.substring(0, 29)}..." else title
            setNick(nick)
        }
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        skips.clear()
        trackPacketLost = 0
        trackPacketsSent = 0

        if (endReason.mayStartNext) {
            playNext()
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
