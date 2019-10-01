package jukebot.commands

import jukebot.JukeBot
import jukebot.framework.Command
import jukebot.framework.CommandChecks
import jukebot.framework.CommandProperties
import jukebot.framework.Context
import jukebot.utils.TextSplitter
import net.dv8tion.jda.api.EmbedBuilder

@CommandProperties(description = "Displays lyrics for the currently playing song")
@CommandChecks.Playing
class Lyrics : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        val player = JukeBot.getPlayer(context.guild.idLong)

        if (JukeBot.kSoftAPI == null) {
            context.embed("Not Configured", "The Lyrics API has not been configured.\n" +
                "This feature is unavailable.")
            return
        }

        val query = player.player.playingTrack.info.title

        JukeBot.kSoftAPI.getLyrics(query) {
            if (it == null || 25 > it.score) {
                return@getLyrics context.embed("No Lyrics Found", "The API returned no lyrics for **$query**")
            }

            val title = "${it.track} by ${it.artist}"
            val pages = TextSplitter.split(it.lyrics)

            if (pages.isEmpty() || pages.size > 4) {
                return@getLyrics context.embed("No Lyrics Found", "The API returned no lyrics for **$query**")
            }

            sendChunks(context, title, pages)
        }
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

}