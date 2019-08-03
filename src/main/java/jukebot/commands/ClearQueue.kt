package jukebot.commands

import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(description = "Removes all of the tracks from the queue", aliases = ["cq", "c", "clear", "empty"], category = CommandProperties.category.MEDIA)
class ClearQueue : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (player.queue.isEmpty()) {
            return context.embed("Nothing to Clear", "The queue is already empty!")
        }

        if (!context.isDJ(true)) {
            return context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.serux.pro/faq)")
        }

        player.queue.clear()
        context.embed("Queue Cleared", "The queue is now empty.")
    }

}
