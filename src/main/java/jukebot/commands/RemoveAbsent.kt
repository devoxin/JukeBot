package jukebot.commands

import jukebot.framework.*

@CommandProperties(
    description = "Removes songs added by members absent from the VoiceChannel",
    aliases = ["ra"],
    category = CommandCategory.QUEUE
)
@CommandCheck(dj = DjCheck.ROLE_ONLY)
class RemoveAbsent : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        val handler = context.getAudioPlayer()

        if (handler.queue.isEmpty()) {
            return context.embed("Queue Empty", "There are no tracks to remove.")
        }

        val membersInVc = context.guild.audioManager.connectedChannel!!.members.map { it.idLong }
        val tracksToRemove = handler.queue
            .filter { !membersInVc.contains(it.userData as Long) }

        handler.queue.removeAll(tracksToRemove)
        context.embed("Queue Cleaned", "Removed **${tracksToRemove.size}** tracks queued by absent members.")
    }

}