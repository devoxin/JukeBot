package me.devoxin.jukebot.commands.misc

import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.framework.Command
import me.devoxin.jukebot.framework.CommandProperties
import me.devoxin.jukebot.framework.Context
import me.devoxin.jukebot.utils.TextSplitter
import me.devoxin.jukebot.utils.json
import me.devoxin.jukebot.utils.toMessage
import net.dv8tion.jda.api.EmbedBuilder
import java.net.URLEncoder
import java.util.concurrent.CompletableFuture

@CommandProperties(description = "Searches for lyrics.")
class Lyrics : Command(ExecutionType.STANDARD) {
    override fun execute(context: Context) {
        val player = JukeBot.getPlayer(context.guild.idLong)

        if (!player.isPlaying && context.args.isEmpty) {
            return context.embed("Lyrics", "Play something, or specify the title of a song.")
        }

        val query = context.args.gatherNext("query").ifEmpty { player.player.playingTrack.info.title }
        val encoded = URLEncoder.encode(query, Charsets.UTF_8)

        getLyrics(encoded).thenAccept {
            when {
                !it.exists -> return@thenAccept context.embed(
                    "No Lyrics Found",
                    "The API returned no lyrics for **$query**"
                )
                it.lyrics == null -> return@thenAccept context.embed(
                    "Lyrics",
                    "The API did not provide a valid response."
                )
                else -> {
                    val pages = TextSplitter.split(it.lyrics)

                    if (pages.isEmpty() || pages.size > 5) {
                        return@thenAccept context.embed("No Lyrics Found", "The API returned no lyrics for **$query**")
                    }

                    sendChunks(context, it.title!!, pages)
                }
            }
        }.exceptionally {
            context.embed("Lyrics", "An unknown error occurred while fetching lyrics.")
            return@exceptionally null
        }
    }

    private fun getLyrics(query: String): CompletableFuture<LyricsResult> {
        val future = CompletableFuture<LyricsResult>()

        JukeBot.httpClient.get(LYRICS_BASE_URL + query).queue({
            if (it.code == 404) {
                it.close()
                return@queue future.complete(LyricsResult(false, null, null)).let {}
            }

            val response = it.json()
                ?: return@queue future.complete(LyricsResult(true, null, null)).let {}

            val content = response.getString("lyrics")
            val title = response.getString("name")

            future.complete(LyricsResult(true, title, content))
        }, future::completeExceptionally)

        return future
    }

    private fun sendChunks(context: Context, title: String, chunks: Array<String>, index: Int = 0) {
        context.channel.sendMessage(
            EmbedBuilder().apply {
                setColor(context.embedColor)
                setTitle(title)
                setDescription(chunks[index])
                build()
            }.build().toMessage()
        ).queue {
            if (chunks.size > index + 1) {
                sendChunks(context, title, chunks, index + 1)
            }
        }
    }

    inner class LyricsResult(val exists: Boolean, val title: String?, val lyrics: String?)

    companion object {
        private const val LYRICS_BASE_URL = "https://evan.lol/lyrics/search/top?q="
    }
}
