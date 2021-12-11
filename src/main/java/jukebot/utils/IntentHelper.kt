package jukebot.utils

import net.dv8tion.jda.api.requests.GatewayIntent

object IntentHelper {
    private val disabledIntents = listOf(
        GatewayIntent.DIRECT_MESSAGE_REACTIONS,
        GatewayIntent.DIRECT_MESSAGES,
        GatewayIntent.DIRECT_MESSAGE_TYPING,
        GatewayIntent.GUILD_INVITES,
        GatewayIntent.GUILD_BANS,
        GatewayIntent.GUILD_EMOJIS,
        GatewayIntent.GUILD_MEMBERS,
        GatewayIntent.GUILD_MESSAGE_REACTIONS,
        GatewayIntent.GUILD_MESSAGE_TYPING,
        GatewayIntent.GUILD_PRESENCES
    )

    // Basically everything except GUILD_MESSAGES + GUILD_VOICE_STATES

    val allIntents = GatewayIntent.ALL_INTENTS
    val disabledIntentsInt = GatewayIntent.getRaw(disabledIntents)
    val enabledIntentsInt = allIntents and disabledIntentsInt.inv()
    val enabledIntents = GatewayIntent.getIntents(enabledIntentsInt)
}
