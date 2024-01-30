package me.devoxin.jukebot.commands

import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.entities.Cog
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.extensions.await

class Developer : Cog {
    @Command(description = "Re-sync commands with Discord.", developerOnly = true)
    suspend fun resync(ctx: Context) {
        val commands = ctx.commandClient.commands.toDiscordCommands()
        ctx.asSlashContext?.deferAsync(true)
        ctx.jda.updateCommands().addCommands(commands).await()
        ctx.respond("Re-synced **${commands.size}** commands.")
    }

    @Command(description = "Un/block a user.", developerOnly = true)
    fun setBlocked(ctx: Context, userId: Long, blocked: Boolean) {
        Database.setIsBlocked(userId, blocked)
        ctx.respond("User ${if (blocked) "blocked" else "unblocked."}")
    }
}
