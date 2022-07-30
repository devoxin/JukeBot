package jukebot.commands.queue

import jukebot.framework.*

@CommandProperties(description = "Moves a track in the queue", aliases = ["m", "mv"], category = CommandCategory.QUEUE)
@CommandChecks.Dj(alone = true)
class Move : Command(ExecutionType.STANDARD) {
    override fun execute(context: Context) {
        val player = context.audioPlayer

        if (player.queue.isEmpty()) {
            return context.embed("Queue is empty", "There are no tracks to move.")
        }

        val args = context.args

        if (args.size < 2) {
            return context.embed("Specify track index", "You need to specify the index of the track in the queue.")
        }

        val target = args[0].toIntOrNull()?.takeIf { it > 0 && it <= player.queue.size }
            ?: return context.embed("Move Tracks", "Invalid target track. Example: `move 3 1`")
        val dest = args[1].toIntOrNull()?.takeIf { it > 0 && it <= player.queue.size && it != target }
            ?: return context.embed("Move Tracks", "Invalid destination position. Example: `move 3 1`")

        val selectedTrack = player.queue[target - 1]

        player.queue.removeAt(target - 1)
        player.queue.add(dest - 1, selectedTrack)
        context.embed("Track Moved", "**${selectedTrack.info.title}** is now at position **$dest** in the queue")
    }
}
