package me.devoxin.jukebot.audio

import com.sedmelluq.discord.lavaplayer.player.FunctionalResultHandler
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.Launcher
import me.devoxin.jukebot.utils.collections.FixedSet
import java.util.concurrent.CompletableFuture

class AutoPlay(private val guildId: Long) {
    private val seedTracks = FixedSet<AudioTrack>(5)

    val enabled: Boolean
        get() = Database.getIsPremiumServer(guildId) && Database.getIsAutoPlayEnabled(guildId)

    val isUsable: Boolean
        get() = seedTracks.size > 0

    fun store(track: AudioTrack) {
        seedTracks.add(track)
    }

    fun getRelatedTrack(): AudioTrack? {
        if (!enabled) {
            return null
        }

        val future = CompletableFuture<AudioTrack?>()

        Launcher.playerManager.loadItem(
            "sprec:${seedTracks.joinToString(",") { it.identifier }}",
            FunctionalResultHandler(future::complete, { future.complete(it.tracks.firstOrNull()) }, { future.complete(null) }, { future.complete(null) })
        )

        return future.get()?.also { it.userData = Launcher.shardManager.botId }
    }
}
