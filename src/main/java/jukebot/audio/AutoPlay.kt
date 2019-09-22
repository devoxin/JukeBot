package jukebot.audio

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jukebot.Database
import jukebot.JukeBot
import java.util.concurrent.CompletableFuture

class AutoPlay(private val guildId: Long) {

    val trackIds = mutableSetOf<String>()

    val enabled: Boolean
        get() = Database.isPremiumServer(guildId) && Database.getIsAutoPlayEnabled(guildId)

    val hasSufficientData: Boolean
        get() = trackIds.size > 0

    fun store(identifier: String) {
        if (trackIds.add(identifier) && trackIds.size > MAX_SET_SIZE) {
            trackIds.remove(trackIds.first())
        }
    }

    fun getRelatedTrack(): CompletableFuture<AudioTrack> {
        val fut = CompletableFuture<AudioTrack>()

        JukeBot.kSoftAPI.getMusicRecommendations(*trackIds.toTypedArray())
            .thenAccept { tr ->
                JukeBot.youTubeApi.getVideoInfo(tr.id)
                    .thenAccept {
                        it.userData = JukeBot.selfId
                        fut.complete(it)
                    }
                    .exceptionally {
                        fut.completeExceptionally(it)
                        return@exceptionally null
                    }
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
