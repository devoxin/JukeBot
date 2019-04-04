package jukebot

import jukebot.utils.Helpers
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.util.*
import java.util.concurrent.TimeUnit

class ActionWaiter : ListenerAdapter() {

    private val selectionMenus = HashMap<Long, (String?) -> Unit>()

    fun waitForSelection(userID: Long, selection: (String?) -> Unit, delay: Int = 10, unit: TimeUnit = TimeUnit.SECONDS) {
        selectionMenus[userID] = selection
        Helpers.schedule({
            if (selectionMenus.containsValue(selection)) {
                selectionMenus.remove(userID)?.invoke(null)
            }
        }, delay, unit)
    }

    override fun onGuildMessageReceived(e: GuildMessageReceivedEvent) {
        selectionMenus.remove(e.author.idLong)?.invoke(e.message.contentDisplay)
    }

}
