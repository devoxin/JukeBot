package jukebot.commands

import jukebot.utils.*

@CommandProperties(description = "Displays the current queue", aliases = ["q", "list", "songs"], category = CommandProperties.category.MEDIA)
class Queue : Command {

    override fun execute(context: Context) {

        val player = context.getAudioPlayer()
        val queue = player.queue

        if (queue.isEmpty()) {
            context.embed("Queue is empty", "There are no tracks to display.\nUse `${context.prefix}now` to view current track.")
            return
        }

        val queueDuration = queue.map { it.duration }.sum().toTimeString()
        val fQueue = StringBuilder()

        val selectedPage = context.args.getOrNull(0)?.toInt() ?: 1

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
