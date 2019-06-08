package jukebot.commands

import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import jukebot.JukeBot
import jukebot.apis.SpotifyAPI
import jukebot.audio.AudioHandler
import jukebot.audio.SongResultHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import java.util.concurrent.CompletableFuture

@CommandProperties(description = "Loads a playlist from Spotify", category = CommandProperties.category.CONTROLS)
public class Spotify : Command(ExecutionType.TRIGGER_CONNECT) { // TODO: Consider moving this to `play` eventually

    override fun execute(context: Context) {
        if (context.donorTier < 2 && !JukeBot.isSelfHosted) {
            return context.embed("Spotify Unavailable", "You must be a [donor in Tier 2 or higher](https://patreon.com/Devoxin)")
        }

        val url = context.getArg(0).replace("<", "").replace(">", "")

        if (url.isEmpty()) {
            return context.embed("Spotify", "You need to specify a URL to a playlist")
        }

        if (!JukeBot.spotifyApi.isEnabled()) {
            return context.embed("Spotify", "Spotify support is unavailable as no valid client ID/client secret was provided.")
        }

        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            player.setChannel(context.channel.idLong)
        }

        loadSpotifyPlaylist(context, player, url)
    }

    private fun loadSpotifyPlaylist(ctx: Context, player: AudioHandler, url: String) {
        val loadHandler = SongResultHandler(ctx, player, false)
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
