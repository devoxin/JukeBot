package jukebot.commands

import jukebot.framework.*

@CommandProperties(
    aliases = ["dedupe", "dd"],
    description = "Removes all duplicate tracks from the queue",
    category = CommandCategory.QUEUE
)
@CommandCheck(dj = DjCheck.ROLE_OR_ALONE)
class DeDuplicate : Command(ExecutionType.REQUIRE_MUTUAL) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (player.queue.isEmpty()) {
            return context.embed("Queue is empty", "There is nothing to de-duplicate.")
        }

        val originalSize = player.queue.size
        val ids = HashSet<String>()
        player.queue.removeIf { !ids.add(it.identifier) }

        val removed = originalSize - player.queue.size
        context.embed("Queue De-duplicated", "Removed $removed duplicate tracks.")
    }

}