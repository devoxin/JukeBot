package me.devoxin.jukebot.commands

import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.context.SlashContext
import me.devoxin.flight.api.entities.Cog
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.Launcher
import me.devoxin.jukebot.audio.sources.deezer.DeezerAudioSourceManager
import me.devoxin.jukebot.extensions.await

class Developer : Cog {
    override fun name() = "Developer"

    @Command(description = "Developer commands.", developerOnly = true)
    fun developer(ctx: SlashContext) {
        ctx.reply("Should you be here?", true)
    }

    @SubCommand(description = "Re-sync commands with Discord.")
    suspend fun resync(ctx: Context) {
        val commands = ctx.commandClient.commands.toDiscordCommands()
        ctx.asSlashContext?.deferAsync(true)
        ctx.jda.updateCommands().addCommands(commands).await()
        ctx.respond("Re-synced **${commands.size}** commands.")
    }

    @SubCommand(description = "Un/block a user.")
    fun setBlocked(ctx: SlashContext, userId: Long, blocked: Boolean) {
        Database.setIsBlocked(userId, blocked)
        ctx.reply("User ${if (blocked) "blocked" else "unblocked."}", true)
    }

    @SubCommand(description = "Set a new source token.")
    fun setSourceToken(ctx: SlashContext, sourceToken: String) {
        val source = Launcher.playerManager.source(DeezerAudioSourceManager::class.java)
            ?: return ctx.reply("Source not found.", true)

        source.arl = sourceToken
        ctx.reply("Source token updated.", true)
    }
}
