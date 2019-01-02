package jukebot.commands

import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.toTimeString

@CommandProperties(description = "Displays the current queue", aliases = ["q", "list", "songs"], category = CommandProperties.category.MEDIA)
class Queue : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {

        val player = context.getAudioPlayer()
        val queue = player.queue

        if (queue.isEmpty()) {
            return context.embed("Queue is empty", "There are no tracks to display.\nUse `${context.prefix}now` to view current track.")
        }

        val queueDuration = queue.map { it.duration }.sum().toTimeString()
        val fQueue = StringBuilder()

        val selectedPage = context.args.getOrNull(0)?.toIntOrNull() ?: 1

        val maxPages = Math.ceil(queue.size.toDouble() / 10).toInt()
        val page = Math.min(Math.max(selectedPage, 1), maxPages)

        val begin = (page - 1) * 10
        val end = if (begin + 10 > queue.size) queue.size else begin + 10

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
