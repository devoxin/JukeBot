package me.devoxin.jukebot.commands

import com.sedmelluq.discord.lavaplayer.natives.opus.OpusEncoderLibrary.*
import me.devoxin.flight.api.annotations.Autocomplete
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.context.SlashContext
import me.devoxin.flight.api.entities.Cog
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.Launcher
import me.devoxin.jukebot.audio.sources.deezer.DeezerAudioSourceManager
import me.devoxin.jukebot.extensions.audioPlayer
import me.devoxin.jukebot.extensions.await
import me.devoxin.jukebot.extensions.respondUnit
import me.devoxin.jukebot.utils.StringUtils
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice

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

    @SubCommand(description = "Configure per-player opus encoder options.")
    fun configureOpusEncoder(ctx: Context, @Autocomplete("opusRequestAutocomplete") request: Int, value: Int) {
        val player = ctx.audioPlayer
            ?: return ctx.respondUnit("No player here.")

        player.player.configuration.opusEncoderConfiguration.configureRaw(request, value)
        ctx.respond("Configuration applied. Changes will take effect at the start of a new track.")
    }

    fun opusRequestAutocomplete(event: CommandAutoCompleteInteractionEvent) {
        val typed = event.focusedOption.value
        val matches = OPUS_REQUESTS.entries.filter { StringUtils.isSubstringWithin(it.key, typed) }
            .map { Choice(it.key, it.value.toLong()) }

        return event.replyChoices(matches).queue()
    }

    companion object {
        private val OPUS_REQUESTS = mapOf(
            "SET_VBR_REQUEST" to SET_VBR_REQUEST,
            "SET_BITRATE_REQUEST" to SET_BITRATE_REQUEST,
            "SET_VBR_CONSTRAINT_REQUEST" to SET_VBR_CONSTRAINT_REQUEST
        )
    }
}
