package jukebot.audio

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jukebot.Database
import jukebot.JukeBot
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
        return JukeBot.kSoftAPI.getMusicRecommendations(*trackTitles.toTypedArray())
            .thenCompose(JukeBot.playerManager::searchYoutube)
            .thenApply { it.apply { userData = JukeBot.selfId } }
    }

    companion object {
        private const val MAX_SET_SIZE = 5
    }

}
