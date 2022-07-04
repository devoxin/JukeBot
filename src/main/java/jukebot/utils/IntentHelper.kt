package jukebot.utils

import net.dv8tion.jda.api.requests.GatewayIntent

object IntentHelper {
    private val disabledIntents = listOf(
        GatewayIntent.DIRECT_MESSAGES,
        GatewayIntent.DIRECT_MESSAGE_REACTIONS,
        GatewayIntent.DIRECT_MESSAGE_TYPING,
        GatewayIntent.GUILD_BANS,
        GatewayIntent.GUILD_EMOJIS,
        GatewayIntent.GUILD_INVITES,
        GatewayIntent.GUILD_MEMBERS,
        GatewayIntent.GUILD_MESSAGE_REACTIONS,
        GatewayIntent.GUILD_MESSAGE_TYPING,
        GatewayIntent.GUILD_PRESENCES,
        GatewayIntent.GUILD_WEBHOOKS
    )

    // Basically everything except GUILD_MESSAGES + GUILD_VOICE_STATES

    private val disabledIntentsInt = GatewayIntent.getRaw(disabledIntents)
    private val enabledIntentsInt = GatewayIntent.ALL_INTENTS and disabledIntentsInt.inv()
    val enabledIntents = GatewayIntent.getIntents(enabledIntentsInt)
}
