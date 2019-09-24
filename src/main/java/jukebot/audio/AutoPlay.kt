package jukebot.audio

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jukebot.Database
import jukebot.JukeBot
import java.util.concurrent.CompletableFuture

class AutoPlay(private val guildId: Long) {

    private val trackTitles = mutableSetOf<String>()

    val enabled: Boolean
        get() = Database.isPremiumServer(guildId) && Database.getIsAutoPlayEnabled(guildId)

    val hasSufficientData: Boolean
        get() = trackTitles.size > 0

    fun store(identifier: String) {
        if (trackTitles.add(identifier) && trackTitles.size > MAX_SET_SIZE) {
            trackTitles.remove(trackTitles.first())
        }
    }

    fun getRelatedTrack(): CompletableFuture<AudioTrack> {
        val fut = CompletableFuture<AudioTrack>()

        JukeBot.kSoftAPI.getMusicRecommendations(*trackTitles.toTypedArray())
            .thenCompose(JukeBot.youTubeApi::getVideoInfo)
            .thenAccept {
                it.userData = JukeBot.selfId
                fut.complete(it)
            }
            .exceptionally {
                fut.completeExceptionally(it)
                return@exceptionally null
            }

        return fut
    }

    companion object {
        private const val MAX_SET_SIZE = 5
    }

}
