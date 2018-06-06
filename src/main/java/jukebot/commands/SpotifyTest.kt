package jukebot.commands

import jukebot.JukeBot
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

        val match: Matcher = playlistRegex.matcher(context.argString)

        if (!match.find()) {
            return context.sendEmbed("Invalid Link", "You need to pass a valid Spotify playlist link!")
        }

        context.sendEmbed("Please wait", "Fetching playlist tracks...")

        JukeBot.spotifyApi.getTracksFromPlaylist(match.group(1), match.group(2), { tracks ->
            if (tracks.isEmpty()) {
                return@getTracksFromPlaylist context.sendEmbed("No Tracks Found", "No tracks returned by the API.\nCheck that the playlist is public and there are songs in the list")
            }

            val shortList = tracks.subList(0, Math.min(tracks.size, 10))
            context.sendEmbed("The first 10 tracks...", shortList.joinToString("\n") { "${it.artist} - ${it.name}" })
        })
    }
}
