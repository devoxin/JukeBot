package me.devoxin.jukebot.commands.queue

import me.devoxin.jukebot.framework.Command
import me.devoxin.jukebot.framework.CommandCategory
import me.devoxin.jukebot.framework.CommandProperties
import me.devoxin.jukebot.framework.Context

@CommandProperties(aliases = ["z"], description = "Removes the last song queued by you", category = CommandCategory.QUEUE)
class Undo : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val handler = context.audioPlayer

        if (handler.queue.isEmpty()) {
            return context.embed("Nothing to Remove", "The queue is empty!")
        }

        val queue = handler.queue
        val i = queue.descendingIterator()

        while (i.hasNext()) {
            val t = i.next()
            val requester = t.userData as Long

            if (requester == context.author.idLong) {
                i.remove()
                return context.embed("Track Removed", "**${t.info.title}** removed from the queue.")
            }
        }

        context.embed("No Tracks Found", "No tracks queued by you were found.")
    }
}