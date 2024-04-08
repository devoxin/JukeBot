package me.devoxin.jukebot.integrations.patreon

//  pledge campaign
enum class PatreonTier(val tierId: Int, val tierName: String, val tierAmountCents: Int, val entitledPremiumServers: Int) {
    UNKNOWN(0, "Unknown", 0, 0),
    PERSONAL(22704575, "Personal", 499, 0),
    SERVER(22704590, "Server", 999, 3),
    DEVELOPER(999999999, "Developer", Int.MAX_VALUE, Int.MAX_VALUE); // literally like a game cheat code haha!

    companion object {
        fun from(tierId: Int) = PatreonTier.entries.firstOrNull { it.tierId == tierId } ?: UNKNOWN
    }
}
