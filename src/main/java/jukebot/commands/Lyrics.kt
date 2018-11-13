package jukebot.commands

import jukebot.JukeBot
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.TextSplitter
import net.dv8tion.jda.core.EmbedBuilder

@CommandProperties(description = "Displays lyrics for the currently playing song")
class Lyrics : Command {

    override fun execute(context: Context) {
        val player = JukeBot.getPlayer(context.guild.audioManager)

        if (!player.isPlaying) {
            context.embed("Not Playing", "Nothing is currently playing.")
            return
        }

        val query = player.player.playingTrack.info.title

        JukeBot.kSoftAPI.getLyrics(query) {
            if (it == null || 25 > it.score) {
                return@getLyrics context.embed("No Lyrics Found", "The API returned no lyrics for **$query**")
            }

            val title = "${it.track} by ${it.artist}"
            val pages = TextSplitter.split(it.lyrics)

            sendChunks(context, title, pages)
        }
    }

    private fun sendChunks(context: Context, title: String, chunks: Array<String>, index: Int = 0) {
        context.channel.sendMessage(EmbedBuilder()
                .setColor(JukeBot.embedColour)
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