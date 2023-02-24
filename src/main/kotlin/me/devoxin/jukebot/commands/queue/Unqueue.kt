package me.devoxin.jukebot.commands.queue

import me.devoxin.jukebot.framework.*
import me.devoxin.jukebot.utils.Constants
import net.dv8tion.jda.api.interactions.commands.OptionType

@CommandProperties(description = "Remove a track from the queue.", aliases = ["uq", "remove", "r", "rm"], category = CommandCategory.QUEUE)
@Option(name = "position", description = "The type position of the track in the queue to remove.", type = OptionType.INTEGER)
class Unqueue : Command(ExecutionType.STANDARD) {
    override fun execute(context: Context) {
        val player = context.audioPlayer

        if (player.queue.isEmpty()) {
            return context.embed("Queue Empty", "There are no tracks to remove.")
        }

        if (context.args.isEmpty) {
            return context.embed("Specify track index", "You need to specify the index of the track to remove.")
        }

        // TODO: Accept a range? i.e. 3-7, and/or multiple numbers, i.e. 3 8 11 12 21
        val selected = context.args.next("position", ArgumentResolver.INTEGER)?.takeIf { it > 0 && it <= player.queue.size }
            ?: return context.embed("Invalid position specified!", "You need to specify a valid target track.")

        val selectedTrack = player.queue[selected - 1]

        if (selectedTrack.userData as Long != context.author.idLong && !context.isDJ(false)) {
            return context.embed(
                "Not a DJ",
                "You need the DJ role to unqueue others' tracks. [See here on how to become a DJ](${Constants.WEBSITE}/faq)"
            )
        }

        player.queue.removeAt(selected - 1)
        context.embed("Track Unqueued", "Removed **${selectedTrack.info.title}** from the queue.")
    }
}
