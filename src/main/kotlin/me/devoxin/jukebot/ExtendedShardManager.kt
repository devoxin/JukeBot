package me.devoxin.jukebot

import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.GatewayIntent.*
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag.*
import java.util.*
import net.dv8tion.jda.api.requests.GatewayIntent.SCHEDULED_EVENTS as GATEWAY_SCHEDULED_EVENTS

class ExtendedShardManager(private val shardManager: ShardManager, val botId: Long): ShardManager by shardManager {
    companion object {
        private val disabledIntents = EnumSet.of(
            AUTO_MODERATION_CONFIGURATION,
            AUTO_MODERATION_EXECUTION,
            DIRECT_MESSAGES,
            DIRECT_MESSAGE_REACTIONS,
            DIRECT_MESSAGE_TYPING,
            GUILD_EMOJIS_AND_STICKERS,
            GUILD_INVITES,
            GUILD_MEMBERS,
            GUILD_MESSAGE_REACTIONS,
            GUILD_MESSAGE_TYPING,
            GUILD_MODERATION,
            GUILD_PRESENCES,
            GUILD_WEBHOOKS,
            GATEWAY_SCHEDULED_EVENTS
        )

        private val defaultEnabledIntents = EnumSet.complementOf(disabledIntents)

        fun create(token: String,
                   enableIntents: Collection<GatewayIntent> = listOf(),
                   configurator: DefaultShardManagerBuilder.() -> Unit = {}): ExtendedShardManager {
            val shardManager = DefaultShardManagerBuilder.create(token, defaultEnabledIntents)
                .setShardsTotal(-1)
                .setMemberCachePolicy(MemberCachePolicy.VOICE)
                .disableCache(ACTIVITY, CLIENT_STATUS, EMOJI, FORUM_TAGS, ROLE_TAGS, ONLINE_STATUS, SCHEDULED_EVENTS, STICKER)
                .setBulkDeleteSplittingEnabled(false)
                .enableIntents(enableIntents)
                .apply(configurator)
                .build()

            return ExtendedShardManager(shardManager, parseUserIdFromToken(token))
        }

        private fun parseUserIdFromToken(token: String): Long {
            return Base64.getDecoder().decode(token.split('.')[0]).let(::String).toLong()
        }
    }
}
