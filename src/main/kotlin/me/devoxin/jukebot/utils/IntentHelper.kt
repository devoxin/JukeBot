package me.devoxin.jukebot.utils

import me.devoxin.jukebot.JukeBot
import net.dv8tion.jda.api.requests.GatewayIntent
import java.util.*

object IntentHelper {
    private val disabledIntents: List<GatewayIntent> by lazy {
        val intents = mutableListOf(
            GatewayIntent.AUTO_MODERATION_CONFIGURATION,
            GatewayIntent.AUTO_MODERATION_EXECUTION,
            GatewayIntent.DIRECT_MESSAGES,
            GatewayIntent.DIRECT_MESSAGE_REACTIONS,
            GatewayIntent.DIRECT_MESSAGE_TYPING,
            GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
            GatewayIntent.GUILD_INVITES,
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_MESSAGE_REACTIONS,
            GatewayIntent.GUILD_MESSAGE_TYPING,
            GatewayIntent.GUILD_MODERATION,
            GatewayIntent.GUILD_PRESENCES,
            GatewayIntent.GUILD_WEBHOOKS,
            GatewayIntent.SCHEDULED_EVENTS
        )

        if (JukeBot.selfId == 249303797371895820L) {
            intents.add(GatewayIntent.MESSAGE_CONTENT)
        }

        intents.toList()
    }

    // Basically everything except GUILD_MESSAGES + GUILD_VOICE_STATES

    private val disabledIntentsInt: Int by lazy { GatewayIntent.getRaw(disabledIntents) }
    private val enabledIntentsInt: Int by lazy { GatewayIntent.ALL_INTENTS and disabledIntentsInt.inv() }
    val enabledIntents: EnumSet<GatewayIntent> by lazy { GatewayIntent.getIntents(enabledIntentsInt) }
}
