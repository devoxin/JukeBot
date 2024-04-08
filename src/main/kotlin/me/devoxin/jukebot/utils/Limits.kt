package me.devoxin.jukebot.utils

import me.devoxin.flight.api.context.Context
import me.devoxin.jukebot.Launcher.isSelfHosted
import me.devoxin.jukebot.extensions.premiumUser

object Limits {
    fun loadedTracks(ctx: Context): Int {
        val isOwner = ctx.author.idLong in ctx.commandClient.ownerIds

        return when {
            isSelfHosted || isOwner -> Int.MAX_VALUE
            ctx.premiumUser != null -> Int.MAX_VALUE
            else -> 100
        }
    }

    fun customPlaylists(ctx: Context): Int {
        val isOwner = ctx.author.idLong in ctx.commandClient.ownerIds
        val premiumUser = ctx.premiumUser

        return when {
            isSelfHosted || isOwner -> Int.MAX_VALUE
            premiumUser != null && !premiumUser.shared -> 100
            else -> 3
        }
    }

    fun customPlaylistTracks(ctx: Context): Int {
        val isOwner = ctx.author.idLong in ctx.commandClient.ownerIds
        val premiumUser = ctx.premiumUser

        return when {
            isSelfHosted || isOwner -> Int.MAX_VALUE
            premiumUser != null && !premiumUser.shared -> 1000
            else -> 100
        }
    }
}
