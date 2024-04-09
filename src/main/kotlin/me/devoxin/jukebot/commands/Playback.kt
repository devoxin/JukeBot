package me.devoxin.jukebot.commands

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import kotlinx.coroutines.launch
import me.devoxin.flight.api.annotations.*
import me.devoxin.flight.api.annotations.choice.StringChoice
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.entities.Cog
import me.devoxin.jukebot.Launcher
import me.devoxin.jukebot.annotations.Checks.DJ
import me.devoxin.jukebot.annotations.Checks.Playing
import me.devoxin.jukebot.annotations.Prerequisites.RequireMutualVoiceChannel
import me.devoxin.jukebot.annotations.Prerequisites.TriggerConnect
import me.devoxin.jukebot.extensions.audioPlayer
import me.devoxin.jukebot.extensions.embed
import me.devoxin.jukebot.extensions.toTimeString
import me.devoxin.jukebot.extensions.truncate
import me.devoxin.jukebot.utils.Scopes
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice

class Playback : Cog {
    override fun name() = "Playback"

    @Command(aliases = ["p"], description = "Find and play a track.", guildOnly = true)
    @TriggerConnect
    fun play(ctx: Context,
             @Autocomplete("autocompletePlaySuggestions")
             @Describe("The query, or URL to search for.")
             @Greedy query: String) { // TODO: Nullable to allow loading attachments.
        play0(ctx, query)
    }

    @Command(aliases = ["pn"], description = "Finds a track and queues it to play next.", guildOnly = true)
    @Playing
    @RequireMutualVoiceChannel
    fun playNext(ctx: Context,
                 @Autocomplete("autocompletePlaySuggestions")
                 @Describe("The query, or URL to search for.")
                 @Greedy query: String) { // TODO: Nullable to allow loading attachments.
        if (ctx.audioPlayer == null) {
            return ctx.embed("Not Playing", "There's no player here. Use the `/play` command instead!")
        }

        play0(ctx, query)
    }

    @Command(aliases = ["search", "sel", "s", "add"], description = "Search and select from up to 5 tracks.", guildOnly = true)
    @TriggerConnect
    fun select(ctx: Context,
               @Autocomplete("autocompletePlaySuggestions")
               @Describe("The query, or URL to search for.")
               @Greedy query: String) {
        play0(ctx, query, useSelection = true)
    }

    @Command(aliases = ["find"], description = "Search for a track.", guildOnly = true)
    @TriggerConnect
    fun search(ctx: Context,
               @Choices(string = [StringChoice("soundcloud", "scsearch"), StringChoice("spotify", "spsearch")])
               @Describe("The source to search.")
               searchProvider: String,
               @Autocomplete("autocompletePlaySuggestions")
               @Describe("The query, or URL to search for.")
               @Greedy query: String) {
        play0(ctx, "$searchProvider:$query", useSelection = true)
    }

    @Command(aliases = ["prev"], description = "Go back to the last played track.", guildOnly = true)
    @DJ(alone = true)
    @RequireMutualVoiceChannel
    fun previous(ctx: Context) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Previous Track", "There's no track to go back to.")

        if (!player.canGoBack) {
            return ctx.embed("No Previous Track", "There's no track to go back to.")
        }

        player.previous()
        ctx.asSlashContext?.reply("Going back to the previous track!")
    }

    private fun checkNsfw(ctx: Context): Boolean {
//        val nsfwTcCheck = (ctx.channel as? VoiceChannel)?.isNSFW == true || (ctx.channel as? TextChannel)?.isNSFW == true
//                val nsfwVcCheck = (ctx.member.voiceState?.channel as? VoiceChannel)?.isNSFW == true
//
//                if (!nsfwTcCheck || !nsfwVcCheck) {
//                    when {
//                        !nsfwTcCheck -> ctx.embed("PornHub Tracks", "PornHub tracks can only be loaded from NSFW channels!")
//                        !nsfwVcCheck -> ctx.embed("PornHub Tracks", "PornHub tracks can only be played in NSFW voice channels!")
//                    }
//
//                    return
//                }
        return false
    }

    private fun play0(ctx: Context, query: String, useSelection: Boolean = false, playNext: Boolean = false) {
        val player = ctx.audioPlayer()
        val userQuery = query.removePrefix("<").removeSuffix(">")
        val identifier: String

        if (userQuery.startsWith("http") || userQuery.startsWith("spotify:")) {
            if ("soundcloud.com/you/" in userQuery.lowercase()) {
                return ctx.embed("SoundCloud Liked Tracks", "Loading SoundCloud tracks requires username.")
            }

            if ("pornhub" in userQuery.lowercase() && Launcher.playerManager.enableNsfw && !checkNsfw(ctx)) {
                return
            }

            identifier = userQuery.split(' ')[0]
        } else if (!searchProviders.any(query::startsWith)) {
            identifier = "spsearch:$userQuery"
        } else {
            identifier = userQuery
        }

        Scopes.IO.launch {
            ctx.asSlashContext?.deferAsync()
            Launcher.playerManager.loadIdentifier(identifier, ctx, player, useSelection, playNext)
        }
    }

    suspend fun autocompletePlaySuggestions(event: CommandAutoCompleteInteractionEvent) {
        val query = event.focusedOption.value

        if (query.isEmpty() || query.startsWith("http") || query.startsWith("spotify:")) {
            return event.replyChoices().queue()
        }

        val searchProvider = event.getOption("search_provider")?.asString
            ?: "spsearch"

        try {
            val searchResults = Launcher.playerManager.loadAsync("$searchProvider:$query") as? AudioPlaylist
                ?: return event.replyChoices().queue()

            event.replyChoices(
                searchResults.tracks.map { Choice("${it.info.author} - ${it.info.title}".truncate(85) + " (${it.duration.toTimeString()})", it.info.uri) }
            ).queue()
        } catch (t: Throwable) {
            event.replyChoices().queue()
        }
    }

    companion object {
        private val searchProviders = setOf("ytsearch:", "ytmsearch:", "dzsearch:", "dzisrc:", "spsearch:", "scsearch:")
    }
}
