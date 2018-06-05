package jukebot

import jukebot.utils.Helpers
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

import java.util.HashMap
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class ActionWaiter : ListenerAdapter() {

    private val selectionMenus = HashMap<Long, Consumer<String?>>()

    fun waitForSelection(userID: Long, selection: Consumer<String?>) {
        if (!selectionMenus.containsKey(userID)) {
            selectionMenus[userID] = selection
            Helpers.schedule({
                selectionMenus.remove(userID)?.accept(null)
            }, 10, TimeUnit.SECONDS)
        }
    }

    override fun onGuildMessageReceived(e: GuildMessageReceivedEvent) {
        selectionMenus.remove(e.author.idLong)?.accept(e.message.contentDisplay)
    }

}
