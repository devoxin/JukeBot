package jukebot.entities

import jukebot.Database
import java.time.Duration
import java.time.Instant

class PremiumGuild(
        val guildId: Long,
        val added: Long
) {

    fun daysSinceAdded(): Long {
        val current = Instant.now()
        val then = Instant.ofEpochMilli(added)
        val since = Duration.between(then, current)

        return since.toDays()
    }

    fun remove() {
        Database.removePremiumServer(guildId)
    }

}