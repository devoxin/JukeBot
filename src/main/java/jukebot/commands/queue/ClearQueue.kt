package jukebot.commands.queue

import jukebot.framework.*

@CommandProperties(description = "Removes all of the tracks from the queue", aliases = ["cq", "c", "clear", "empty"], category = CommandCategory.QUEUE)
@CommandChecks.Dj(alone = true)
class ClearQueue : Command(ExecutionType.STANDARD) {
    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (player.queue.isEmpty()) {
            return context.embed("Nothing to Clear", "The queue is already empty!")
        }

        player.queue.clear()
        context.embed("Queue Cleared", "The queue is now empty.")
    }
}
