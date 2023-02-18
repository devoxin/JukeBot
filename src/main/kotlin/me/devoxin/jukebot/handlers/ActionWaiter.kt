package me.devoxin.jukebot.handlers

import me.devoxin.jukebot.utils.Helpers
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import java.util.concurrent.TimeUnit

class ActionWaiter : EventListener {
    private val selectionMenus = HashMap<Long, (String?) -> Unit>()

    fun waitForSelection(userID: Long, selection: (String?) -> Unit, delay: Int = 10, unit: TimeUnit = TimeUnit.SECONDS) {
        selectionMenus[userID] = selection
        Helpers.schedule({
            if (selectionMenus.containsValue(selection)) {
                selectionMenus.remove(userID)?.invoke(null)
            }
        }, delay, unit)
    }

    override fun onEvent(event: GenericEvent) {
        if (event is MessageReceivedEvent && event.isFromGuild) {
            onGuildMessageReceived(event)
        }
    }

    private fun onGuildMessageReceived(e: MessageReceivedEvent) {
        selectionMenus.remove(e.author.idLong)?.invoke(e.message.contentRaw)
    }
}
