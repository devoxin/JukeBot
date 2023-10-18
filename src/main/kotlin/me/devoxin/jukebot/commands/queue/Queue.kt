package me.devoxin.jukebot.commands.queue

import me.devoxin.jukebot.framework.*
import me.devoxin.jukebot.utils.iterate
import me.devoxin.jukebot.utils.toTimeString
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.math.ceil
import kotlin.math.min

@CommandProperties(
    description = "Displays the current queue.",
    aliases = ["q", "list", "songs"],
    category = CommandCategory.QUEUE,
    slashCompatible = true
)
@Option(name = "page", description = "The page to view.", type = OptionType.INTEGER)
class Queue : Command(ExecutionType.STANDARD) {
    override fun execute(context: Context) {
        val player = context.audioPlayer
        val queue = player.queue

        if (queue.isEmpty()) {
            return context.embed(
                "Queue is empty",
                "There are no tracks to display.\nUse `${context.prefix}now` to view current track."
            )
        }

        val selectedPage = context.args.next("page", ArgumentResolver.INTEGER) ?: 1
        val maxPages = ceil(queue.size.toDouble() / 10).toInt()
        val page = selectedPage.coerceIn(1, maxPages)

        val queueDuration = queue.sumOf { it.duration }.toTimeString()
        val fQueue = buildString {
            val begin = (page - 1) * 10
            val end = min(begin + 10, queue.size)

            for ((i, track) in queue.iterate(begin..end)) {
                append("`${i + 1}.` ")
                append("**[${track.info.title}](${track.info.uri})** ")
                appendLine("<@${track.userData}>")
            }
        }

        context.embed {
            setTitle("Queue (${queue.size} songs, $queueDuration)")
            setDescription(fQueue.trim())
            setFooter("Page $page/$maxPages", null)
        }
    }
}
