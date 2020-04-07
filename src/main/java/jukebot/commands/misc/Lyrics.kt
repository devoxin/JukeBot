package jukebot.commands.misc

import jukebot.JukeBot
import jukebot.framework.Command
import jukebot.framework.CommandChecks
import jukebot.framework.CommandProperties
import jukebot.framework.Context
import jukebot.utils.TextSplitter
import jukebot.utils.json
import net.dv8tion.jda.api.EmbedBuilder
import java.net.URLEncoder

@CommandProperties(description = "Searches for lyrics.")
class Lyrics : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        val player = JukeBot.getPlayer(context.guild.idLong)

        if (!player.isPlaying && context.args.isEmpty()) {
            return context.embed("Lyrics", "Play something, or specify the title of a song.")
        }

        val query = if (context.args.isNotEmpty()) context.argString else player.player.playingTrack.info.title
        val encoded = URLEncoder.encode(query, Charsets.UTF_8)

        JukeBot.httpClient.get(lyricsUrl + encoded).queue({
            if (it.code() == 404) {
                return@queue context.embed("Lyrics", "The API returned no lyrics for **$query**")
            }

            val response = it.json()
                ?: return@queue context.embed("Lyrics", "The API did not provide a valid response.")

            val content = response.getString("content")
            val title = response.getObject("song").getString("full_title")

            val pages = TextSplitter.split(content)

            if (pages.isEmpty() || pages.size > 5) {
                return@queue context.embed("No Lyrics Found", "The API returned no lyrics for **$query**")
            }

            sendChunks(context, title, pages)
        }, {
            context.embed("Lyrics", "An unknown error occurred while fetching lyrics.")
        })
    }

    private fun sendChunks(context: Context, title: String, chunks: Array<String>, index: Int = 0) {
        context.channel.sendMessage(EmbedBuilder()
            .setColor(context.embedColor)
            .setTitle(title)
            .setDescription(chunks[index])
            .setFooter("KSoft.Si API", null)
            .build()
        ).queue {
            if (chunks.size > index + 1) {
                sendChunks(context, title, chunks, index + 1)
            }
        }
    }

    companion object {
        private const val lyricsUrl = "https://lyrics.tsu.sh/v1/?q="
    }

}