package jukebot.audio

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jukebot.Database
import java.util.concurrent.CompletableFuture

class AutoPlay(private val guildId: Long) {
    private val trackTitles = mutableSetOf<String>()

    val enabled: Boolean
        get() = Database.getIsPremiumServer(guildId) && Database.getIsAutoPlayEnabled(guildId)

    val hasSufficientData: Boolean
        get() = trackTitles.size > 0

    fun store(identifier: String) {
        if (trackTitles.add(identifier) && trackTitles.size > MAX_SET_SIZE) {
            trackTitles.remove(trackTitles.first())
        }
    }

    fun getRelatedTrack(): CompletableFuture<AudioTrack> {
        return CompletableFuture.failedFuture(UnsupportedOperationException("Autoplay provider is unavailable"))
    }

    companion object {
        private const val MAX_SET_SIZE = 5
    }
}
