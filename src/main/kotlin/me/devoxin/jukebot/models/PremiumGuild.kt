package me.devoxin.jukebot.models

import me.devoxin.jukebot.Database
import java.time.Duration
import java.time.Instant

class PremiumGuild(val guildId: Long, val added: Long) {
    val daysSinceAdded: Long
        get() = Duration.between(Instant.ofEpochMilli(added), Instant.now()).toDays()

    fun remove() {
        Database.removePremiumServer(guildId)
    }
}
