package me.devoxin.jukebot.utils

import me.devoxin.flight.api.context.Context
import me.devoxin.jukebot.Launcher.isSelfHosted
import me.devoxin.jukebot.extensions.premiumTier
import java.util.concurrent.TimeUnit

object Limits {
    const val MAX_TIER = 3

    fun loadedTracks(ctx: Context): Int {
        val tier = ctx.premiumTier
        val isOwner = ctx.author.idLong in ctx.commandClient.ownerIds


        return when {
            isSelfHosted || isOwner -> Integer.MAX_VALUE
            tier >= 2 -> Integer.MAX_VALUE
            tier == 1 -> 1000
            else      -> 100
        }
    }

    fun duration(ctx: Context): Pair<Boolean, Long> {
        val tier = ctx.premiumTier
        val isOwner = ctx.author.idLong in ctx.commandClient.ownerIds

        return when {
            isSelfHosted || isOwner -> true to Long.MAX_VALUE
            tier >= 2 -> true to Long.MAX_VALUE
            tier == 1 -> true to TimeUnit.HOURS.toMillis(3)
            else      -> false to TimeUnit.MINUTES.toMillis(15)
        }
    }

    fun customPlaylists(ctx: Context): Int {
        val tier = ctx.premiumTier
        val isOwner = ctx.author.idLong in ctx.commandClient.ownerIds

        return when {
            isSelfHosted || isOwner -> Int.MAX_VALUE
            tier >= 2 -> 100
            tier >= 1 -> 50
            else      -> 5
        }
    }


    fun customPlaylistTracks(ctx: Context): Int {
        val tier = ctx.premiumTier
        val isOwner = ctx.author.idLong in ctx.commandClient.ownerIds

        return when {
            isSelfHosted || isOwner -> Int.MAX_VALUE
            tier >= 2 -> 1000
            tier >= 1 -> 500
            else      -> 100
        }
    }
}
