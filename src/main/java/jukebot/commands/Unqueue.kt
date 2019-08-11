package jukebot.commands

import jukebot.framework.Command
import jukebot.framework.CommandCategory
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(description = "Remove a track from the queue", aliases = ["uq", "remove", "r"], category = CommandCategory.QUEUE)
class Unqueue : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (player.queue.isEmpty()) {
            return context.embed("Queue Empty", "There are no tracks to remove.")
        }

        if (context.argString.isEmpty()) {
            return context.embed("Specify track index", "You need to specify the index of the track to remove.")
        }

        // TODO: Accept a range? i.e. 3-7, and/or multiple numbers, i.e. 3 8 11 12 21
        val selected = context.args.firstOrNull()?.toIntOrNull() ?: 0

        if (selected < 1 || selected > player.queue.size) {
            return context.embed("Invalid position specified!", "You need to specify a valid target track.")
        }

        val selectedTrack = player.queue[selected - 1]

        if (selectedTrack.userData as Long != context.author.idLong && !context.isDJ(false)) {
            return context.embed("Not a DJ", "You need the DJ role to unqueue others' tracks. [See here on how to become a DJ](https://jukebot.serux.pro/faq)")
        }

        player.queue.removeAt(selected - 1)
        context.embed("Track Unqueued", "Removed **${selectedTrack.info.title}** from the queue.")
    }
}
