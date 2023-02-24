package me.devoxin.jukebot.commands.queue

import me.devoxin.jukebot.framework.*
import net.dv8tion.jda.api.interactions.commands.OptionType

@CommandProperties(description = "Moves a track in the queue.", aliases = ["m", "mv"], category = CommandCategory.QUEUE, slashCompatible = true)
@Options([
    Option(name = "target_position", description = "The position of the track to move.", type = OptionType.INTEGER),
    Option(name = "destination_position", description = "The new position to move the track to.", type = OptionType.INTEGER)
])
@CommandChecks.Dj(alone = true)
class Move : Command(ExecutionType.STANDARD) {
    override fun execute(context: Context) {
        val player = context.audioPlayer.takeIf { it.queue.isNotEmpty() }
            ?: return context.embed("Queue is empty", "There are no tracks to move.")

        val args = context.args.takeIf { !it.isEmpty }
            ?: return context.embed("Specify track index", "You need to specify the index of the track in the queue.")

        val target = args.next("target_position", ArgumentResolver.INTEGER)?.takeIf { it > 0 && it <= player.queue.size }
            ?: return context.embed("Move Tracks", "Invalid target track. Example: `move 3 1`")
        val dest = args.next("destination_position", ArgumentResolver.INTEGER)?.takeIf { it > 0 && it <= player.queue.size && it != target }
            ?: return context.embed("Move Tracks", "Invalid destination position. Example: `move 3 1`")

        val selectedTrack = player.queue[target - 1]

        player.queue.removeAt(target - 1)
        player.queue.add(dest - 1, selectedTrack)
        context.embed("Track Moved", "**${selectedTrack.info.title}** is now at position **$dest** in the queue")
    }
}
