package me.devoxin.jukebot.integrations.flight

import me.devoxin.flight.api.entities.PrefixProvider
import me.devoxin.jukebot.Database
import net.dv8tion.jda.api.entities.Message

class CustomPrefixProvider : PrefixProvider {
    override fun provide(message: Message): List<String> {
        return listOf(Database.getPrefix(message.guildIdLong), "${message.jda.selfUser.asMention} ")
    }
}
