package jukebot.commands

import jukebot.framework.Command
import jukebot.framework.CommandCategory
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(
        description = "Removes songs added by members absent from the VoiceChannel",
        aliases = ["ra"],
        category = CommandCategory.QUEUE
)
class RemoveAbsent : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        val handler = context.getAudioPlayer()

        if (handler.queue.isEmpty()) {
            return context.embed("Queue Empty", "There are no tracks to remove.")
        }

        if (!context.isDJ(true)) {
            return context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.serux.pro/faq)")
        }

        val membersInVc = context.guild.audioManager.connectedChannel!!.members.map { it.idLong }
        val tracksToRemove = handler.queue
                .filter { !membersInVc.contains(it.userData as Long) }

        handler.queue.removeAll(tracksToRemove)
        context.embed("Queue Cleaned", "Removed **${tracksToRemove.size}** tracks queued by absent members.")
    }

}