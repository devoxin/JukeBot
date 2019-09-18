package jukebot.commands

import jukebot.framework.Command
import jukebot.framework.CommandCategory
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(
        aliases = ["dedupe", "dd"],
        description = "Removes all duplicate tracks from the queue",
        category = CommandCategory.QUEUE
)
class DeDuplicate : Command(ExecutionType.REQUIRE_MUTUAL) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (player.queue.isEmpty()) {
            return context.embed("Queue is empty", "There is nothing to de-duplicate.")
        }

        if (!context.isDJ(true)) {
            context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.serux.pro/faq)")
            return
        }

        val originalSize = player.queue.size
        val ids = HashSet<String>()
        player.queue.removeIf { !ids.add(it.identifier) }

        val removed = originalSize - player.queue.size
        context.embed("Queue De-duplicated", "Removed $removed duplicate tracks.")
    }

}