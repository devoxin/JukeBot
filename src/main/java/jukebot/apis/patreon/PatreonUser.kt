package jukebot.apis.patreon

class PatreonUser(
        val firstName: String,
        val lastName: String,
        val email: String,
        val pledgeCents: Int,
        val isDeclined: Boolean,
        val discordId: Long?
)
