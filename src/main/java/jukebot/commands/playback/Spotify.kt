package jukebot.commands.playback

import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import jukebot.JukeBot
import jukebot.apis.spotify.SpotifyAPI
import jukebot.audio.AudioHandler
import jukebot.audio.SongResultHandler
import jukebot.framework.*
import java.util.concurrent.CompletableFuture

@CommandProperties(description = "Loads a playlist from Spotify", category = CommandCategory.PLAYBACK)
@CommandChecks.Donor(tier = 2)
class Spotify : Command(ExecutionType.TRIGGER_CONNECT) { // TODO: Consider moving this to `play` eventually

    override fun execute(context: Context) {
        val url = context.args.firstOrNull()
            ?.replace("<", "")
            ?.replace(">", "")
            ?: ""

        if (url.isEmpty()) {
            return context.embed("Spotify", "You need to specify a URL to a playlist")
        }

        if (JukeBot.spotifyApi == null || !JukeBot.spotifyApi.isEnabled()) {
            return context.embed("Spotify", "Spotify support is unavailable as no valid client ID/client secret was provided.")
        }

        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            player.channelId = context.channel.idLong
        }

        loadSpotifyPlaylist(context, player, url)
    }

    private fun loadSpotifyPlaylist(ctx: Context, player: AudioHandler, url: String) {
        val loadHandler = SongResultHandler(ctx, url, player, false)
        val match = SpotifyAPI.PLAYLIST_PATTERN.matcher(url)

        if (!match.matches()) {
            return loadHandler.noMatches()
        }

        val listId = match.group(1)

        JukeBot.spotifyApi.getPlaylist(listId).thenAccept { playlist ->
            val tasks = playlist.tracks.map { track -> track.toYoutubeAudioTrack() }
            CompletableFuture.allOf(*tasks.toTypedArray()).handle { _, _ ->
                val tracks = tasks.filter { !it.isCompletedExceptionally }.map { it.get() }
                val loadResult = BasicAudioPlaylist(playlist.name, tracks, null, false)
                loadHandler.playlistLoaded(loadResult)
            }
        }
    }

}
