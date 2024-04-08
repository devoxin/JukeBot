package me.devoxin.jukebot.audio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import io.sentry.Sentry
import io.sentry.event.BreadcrumbBuilder
import io.sentry.event.Event
import io.sentry.event.EventBuilder
import io.sentry.event.interfaces.ExceptionInterface
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import me.devoxin.flight.api.context.Context
import me.devoxin.jukebot.audio.sources.caching.CachingSourceManager
import me.devoxin.jukebot.extensions.embed
import me.devoxin.jukebot.extensions.toTimeString
import me.devoxin.jukebot.utils.Helpers
import me.devoxin.jukebot.utils.Limits
import me.devoxin.jukebot.utils.Scopes
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import java.util.*
import java.util.concurrent.TimeoutException

class LoadResultHandler(
    private val ctx: Context,
    private val identifier: String,
    private val musicManager: AudioHandler,
    private val useSelection: Boolean,
    private val playNext: Boolean = false
) : AudioLoadResultHandler {
    override fun trackLoaded(track: AudioTrack) {
        CachingSourceManager.cache(identifier, track)

        if (!canQueueTrack(track)) {
            return ctx.embed("Track Unavailable", "This track exceeds certain limits. [Remove these limits by donating!](https://patreon.com/Devoxin)")
        }

        val estimatedPlayTime = calculateEstimatedPlayTime()

        if (!musicManager.enqueue(track, ctx.author.idLong, playNext)) {
            ctx.asSlashContext?.respond({
                setContent("The requested track should begin playing shortly.")
            }, true)
            return
        }

        ctx.embed {
            setDescription("**${track.info.title} - ${track.info.author}**\nadded to queue - playing in ~`${estimatedPlayTime.toTimeString()}`")
        }
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        if (playlist.tracks.isEmpty()) {
            return noMatches()
        }

        CachingSourceManager.cache(identifier, playlist)

        if (playlist.isSearchResult) {
            Scopes.IO.launch {
                val selectedTrack = if (!useSelection) playlist.tracks.first() else {
                    val tracks = playlist.tracks
                        .filter(::canQueueTrack)
                        .take(5)
                        .takeIf { it.isNotEmpty() }
                        ?: return@launch noMatches()

                    selectTrack(tracks)
                }

                if (selectedTrack == null) {
                    return@launch ctx.embed("No Selection Made", "The selection menu timed out.\nNo tracks were added to the queue.")
                }

                if (!canQueueTrack(selectedTrack)) {
                    return@launch ctx.embed("Track Unavailable", "This track exceeds certain limits. [Remove these limits by donating!](https://patreon.com/Devoxin)")
                }

                val estimatedPlayTime = calculateEstimatedPlayTime()

                if (!musicManager.enqueue(selectedTrack, ctx.author.idLong, playNext)) {
                    ctx.asSlashContext?.respond({
                        setContent("The requested track should begin playing shortly.")
                    }, true)
                    return@launch
                }

                ctx.embed {
                    setDescription("**${selectedTrack.info.title} - ${selectedTrack.info.author}**\nadded to queue - playing in ~`${estimatedPlayTime.toTimeString()}`")
                }
            }.invokeOnCompletion {
                if (it != null) {
                    Sentry.capture(it)
                    ctx.embed("Error", "An error occurred whilst handling track loading.\nThe error has been logged. Sorry for any inconvenience caused")
                }
            }
        } else {
            val tracks = playlist.tracks
                .filter(::canQueueTrack)
                .take(Limits.loadedTracks(ctx))

            val estPlay = musicManager.queue
                .sumOf { it.duration }
                .plus(musicManager.player.playingTrack?.let { it.duration - it.position } ?: 0)

            for (track in tracks) {
                musicManager.enqueue(track, ctx.author.idLong, false)
            }

            ctx.embed {
                setDescription("**${playlist.name}** - `${tracks.size}` tracks\nadded to queue")

                if (estPlay > 0) {
                    appendDescription(" - playing in ~`${estPlay.toTimeString()}`")
                }
            }
        }
    }

    override fun loadFailed(ex: FriendlyException) {
        val breadCrumb = BreadcrumbBuilder()
            .setCategory("LoadHandler")
            .setMessage("Track ID: $identifier")
            .build()

        val eventBuilder = EventBuilder().withMessage(ex.message)
            .withLevel(Event.Level.ERROR)
            .withSentryInterface(ExceptionInterface(ex))
            .withBreadcrumbs(listOf(breadCrumb))

        Sentry.capture(eventBuilder)
        ctx.embed("Track Unavailable", Helpers.rootCauseOf(ex).localizedMessage)
    }

    override fun noMatches() {
        ctx.embed("No Results", "Nothing found related to the query.")
    }

    private suspend fun selectTrack(tracks: List<AudioTrack>): AudioTrack? {
        val eventId = UUID.randomUUID().toString()
        val selectMenu = StringSelectMenu.create(eventId)

        for ((index, track) in tracks.withIndex()) {
            selectMenu.addOption(track.info.title, index.toString(), "${track.info.author} - ${track.duration.toTimeString()}")
        }

        val prompt = ctx.respond {
            setContent("Select the track you want to queue.")
            setComponents(ActionRow.of(selectMenu.build()))
        }.await()

        try {
            val interaction = ctx.waitFor(
                StringSelectInteractionEvent::class.java,
                { it.componentId == eventId && it.user.idLong == ctx.author.idLong },
                20000
            ).await()

            return tracks[interaction.interaction.values[0].toInt()]
        } catch (e: TimeoutException) {
            return null
        } finally {
            when (prompt) {
                is Message -> prompt.delete().queue()
                is InteractionHook -> prompt.deleteOriginal().queue()
                // else -> {} HUH
            }
        }
    }

    private fun calculateEstimatedPlayTime(): Long {
        val current = musicManager.player.playingTrack
        val remaining = current?.duration?.minus(current.position) ?: 0L
        return if (playNext) remaining else musicManager.queue.sumOf { it.duration } + remaining
    }

    private fun canQueueTrack(track: AudioTrack): Boolean {
        return true
//        val (canQueueStreams, maxTrackDuration) = Limits.duration(ctx)
//        return track.info.isStream && canQueueStreams || maxTrackDuration >= track.duration
    }
}
