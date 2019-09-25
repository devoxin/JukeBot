package jukebot.commands

import jukebot.framework.*

@CommandProperties(description = "Moves a track in the queue", aliases = ["m", "mv"], category = CommandCategory.QUEUE)
@CommandChecks.Dj(alone = true)
class Move : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (player.queue.isEmpty()) {
            return context.embed("Queue is empty", "There are no tracks to move.")
        }

        val args = context.args

        if (args.size < 2) {
            return context.embed("Specify track index", "You need to specify the index of the track in the queue.")
        }

        val target = args[0].toIntOrNull() ?: 0
        val dest = args[1].toIntOrNull() ?: 0

        if (target < 1 || dest < 1 || target == dest || target > player.queue.size || dest > player.queue.size) {
            return context.embed("Invalid position(s) specified!", "You need to specify a valid target track, and a valid target position.")
        }

        val selectedTrack = player.queue[target - 1]

        player.queue.removeAt(target - 1)
        player.queue.add(dest - 1, selectedTrack)

        context.embed("Track Moved", "**${selectedTrack.info.title}** is now at position **$dest** in the queue")

    }
}
