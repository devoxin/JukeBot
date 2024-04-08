package me.devoxin.jukebot.models

import me.devoxin.jukebot.Database
import me.devoxin.jukebot.integrations.patreon.PatreonTier

class PremiumUser(val id: Long,
                  tierId: Int,
                  pledgeAmountCents: Int,
                  var override: Boolean = false,
                  val shared: Boolean = false) {
    var tierId = tierId
        private set

    var pledgeAmountCents = pledgeAmountCents
        private set

    val tier: PatreonTier
        get() = PatreonTier.from(tierId)

    val guilds: List<PremiumGuild>
        get() = Database.getPremiumServersOf(id)

    fun setPledgeAmount(newPledgeAmountCents: Int) {
        pledgeAmountCents = newPledgeAmountCents

        tierId = if (newPledgeAmountCents <= 0) {
            PatreonTier.UNKNOWN.tierId
        } else {
            // assign the highest tier the user is entitled to
            PatreonTier.entries
                .filter { it.tierAmountCents <= newPledgeAmountCents }
                .maxBy { it.tierAmountCents }
                .tierId
        }

        save()
    }

    fun clearPremiumGuilds() {
        for (guild in guilds) {
            guild.remove()
        }
    }

    fun remove() {
        Database.deletePatron(id)
    }

    fun save() {
        Database.setPatron(id, tierId, pledgeAmountCents, override)
    }

    companion object {
        fun fromTier(userId: Long, tier: PatreonTier, override: Boolean = false, shared: Boolean = true): PremiumUser? {
            if (tier == PatreonTier.UNKNOWN) {
                return null
            }

            return PremiumUser(userId, tier.tierId, tier.tierAmountCents, override, shared)
        }
    }
}
