package jukebot.commands

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jukebot.JukeBot
import jukebot.audioutilities.AudioHandler
import jukebot.audioutilities.SpotifyPlaylistLookup
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import java.util.regex.Matcher
import java.util.regex.Pattern

@CommandProperties(description = "Spotify testing :)", developerOnly = true)
class SpotifyTest : Command {

    private val playlistRegex = Pattern.compile("^https://open\\.spotify\\.com/user/([a-zA-Z0-9]+)/playlist/([a-zA-Z0-9]+)")

    override fun execute(context: Context) {
        if (!JukeBot.spotifyApi.isEnabled()) {
            return context.sendEmbed("Spotify Disabled", "Spotify API access is not available on this JukeBot instance.")
        }

        if (context.donorTier < 1) {
            return context.sendEmbed("Feature Unavailable", "Spotify resolving is only available for donors of tier 1+!")
        }

        val connected: Boolean = context.ensureVoice()

        if (!connected) {
            return
        }

        val player: AudioHandler = context.getAudioPlayer()
        val match: Matcher = playlistRegex.matcher(context.argString.replace("<", "").replace(">", ""))

        if (!match.find()) {
            return context.sendEmbed("Invalid Link", "You need to pass a valid Spotify playlist link!")
        }

        context.sendEmbed("Please wait", "Fetching playlist tracks...")

        JukeBot.spotifyApi.getTracksFromPlaylist(match.group(1), match.group(2), { tracks ->
            if (tracks.isEmpty()) {
                return@getTracksFromPlaylist context.sendEmbed("No Tracks Found", "No tracks returned by the API.\nCheck that the playlist is public and there are songs in the list")
            }

            System.out.println("Found ${tracks.size} tracks")

            val audioTracks: List<AudioTrack> = SpotifyPlaylistLookup(tracks).resolveTracks().filterNotNull()

            for (track in audioTracks) {
                player.addToQueue(track, context.author.idLong)
            }

            context.sendEmbed("Spotify Playlist Enqueued", "${audioTracks.size} tracks were added to the queue!")
        })
    }
}
