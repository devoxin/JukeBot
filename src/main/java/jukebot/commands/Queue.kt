package jukebot.commands

import jukebot.framework.Command
import jukebot.framework.CommandCategory
import jukebot.framework.CommandProperties
import jukebot.framework.Context
import jukebot.utils.toTimeString
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

@CommandProperties(description = "Displays the current queue", aliases = ["q", "list", "songs"], category = CommandCategory.QUEUE)
class Queue : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {

        val player = context.getAudioPlayer()
        val queue = player.queue

        if (queue.isEmpty()) {
            return context.embed("Queue is empty", "There are no tracks to display.\nUse `${context.prefix}now` to view current track.")
        }

        val queueDuration = queue.map { it.duration }.sum().toTimeString()
        val fQueue = StringBuilder()

        val selectedPage = context.args.firstOrNull()?.toIntOrNull() ?: 1

        val maxPages = ceil(queue.size.toDouble() / 10).toInt()
        val page = min(max(selectedPage, 1), maxPages)

        val begin = (page - 1) * 10
        val end = min(begin + 10, queue.size)

        for (i in begin until end) {
            val track = queue[i]
            fQueue.append("`")
                    .append(i + 1)
                    .append(".` **[")
                    .append(track.info.title)
                    .append("](")
                    .append(track.info.uri)
                    .append(")** <@")
                    .append(track.userData)
                    .append(">\n")
        }

        context.embed {
            setTitle("Queue (${queue.size} songs, $queueDuration)")
            setDescription(fQueue.toString().trim())
            setFooter("Page $page/$maxPages", null)
        }

    }
}
