package me.devoxin.jukebot.commands.queue

import me.devoxin.jukebot.framework.Command
import me.devoxin.jukebot.framework.CommandCategory
import me.devoxin.jukebot.framework.CommandProperties
import me.devoxin.jukebot.framework.Context
import me.devoxin.jukebot.utils.iterate
import me.devoxin.jukebot.utils.toTimeString
import org.jetbrains.kotlin.utils.addToStdlib.sumByLong
import kotlin.math.ceil
import kotlin.math.min

@CommandProperties(description = "Displays the current queue", aliases = ["q", "list", "songs"], category = CommandCategory.QUEUE)
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

        val selectedPage = context.args.firstOrNull()?.toIntOrNull() ?: 1
        val maxPages = ceil(queue.size.toDouble() / 10).toInt()
        val page = selectedPage.coerceIn(1, maxPages)

        val queueDuration = queue.sumByLong { it.duration }.toTimeString()
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
