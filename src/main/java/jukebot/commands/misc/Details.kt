package jukebot.commands.misc

import com.sedmelluq.discord.lavaplayer.player.FunctionalResultHandler
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jukebot.JukeBot
import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context
import jukebot.utils.toTimeString

@CommandProperties(aliases = ["d", "id"], description = "Show information about a song.")
class Details : Command(ExecutionType.STANDARD) {
    override fun execute(context: Context) {
        if (context.args.isEmpty()) {
            return context.embed("Track Information", "You need to specify a search query.")
        }

        val loadHandler = FunctionalResultHandler(
            { sendTrackInfo(context, it) },
            { sendTrackInfo(context, it.selectedTrack ?: it.tracks.first()) },
            { context.embed("No Results", "Couldn't find any tracks related to the query.") },
            { context.embed("Load Failed", "An error occurred while trying to load track information.") }
        )

        JukeBot.playerManager.loadItem("ytsearch:${context.argString}", loadHandler)
    }

    fun sendTrackInfo(ctx: Context, track: AudioTrack) {
        ctx.embed {
            setColor(ctx.embedColor)
            setTitle(track.info.title, track.info.uri)
            addField("Duration", track.info.length.toTimeString(), true)
            addField("Uploader", track.info.author, true)
            addField("Livestream", if (track.info.isStream) "Yes" else "No", true)
            setThumbnail("https://img.youtube.com/vi/${track.info.identifier}/0.jpg")
        }
    }
}
