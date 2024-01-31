package me.devoxin.jukebot.commands

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.annotations.*
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.entities.Cog
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.Launcher
import me.devoxin.jukebot.annotations.Prerequisites.TriggerConnect
import me.devoxin.jukebot.extensions.audioPlayer
import me.devoxin.jukebot.extensions.embed
import me.devoxin.jukebot.extensions.premiumTier
import me.devoxin.jukebot.utils.Limits
import me.devoxin.jukebot.utils.StringUtils
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice

class Playlists : Cog {
    override fun name() = "Playlists"

    @Command(aliases = ["pl"], description = "Manage your custom playlists.", guildOnly = true)
    fun playlists(ctx: Context) {
        val cmd = ctx.invokedCommand as? CommandFunction
            ?: return

        val padLength = cmd.subcommands.keys.maxOf { it.length }

        val subcommands = cmd.subcommands.values.joinToString("\n") {
            "`${it.name.padEnd(padLength)}:` ${it.properties.description}"
        }

        ctx.embed("Subcommand Required", subcommands)
    }

    @SubCommand(aliases = ["l", "v", "view"], description = "List your custom playlists.")
    fun list(ctx: Context) {
        val playlists = Database.getPlaylists(ctx.author.idLong)

        if (playlists.isEmpty()) {
            return ctx.embed("Custom Playlists", "You don't have any custom playlists.")
        }

        ctx.embed("Custom Playlists", playlists.joinToString("\n"))
    }

    @SubCommand(aliases = ["c", "new"], description = "Create a new custom playlist.")
    fun create(ctx: Context,
               @Autocomplete("autocompletePlaylistName")
               @Describe("The name of your new playlist.")
               @Range(string = [1, 32])
               @Greedy playlistName: String) {
        val playlistCount = Database.countPlaylists(ctx.author.idLong)

        if (!checkPlaylistCount(ctx, playlistCount)) {
            return
        }

        Database.createPlaylist(ctx.author.idLong, playlistName)

        ctx.embed("Custom Playlists (create)", "Playlist created! Any time you hear a song you like, you can add it to your playlist with `/save`!")
    }

    @SubCommand(aliases = ["i"], description = "Import a playlist from an external service.")
    suspend fun import(ctx: Context,
                       @Describe("The URL of the playlist to import.")
                       url: String,
                       @Autocomplete("autocompletePlaylistName")
                       @Describe("The name of the playlist to add the imported tracks to.")
                       @Range(string = [1, 32])
                       @Greedy playlistName: String) {
        val allPlaylists = Database.getPlaylists(ctx.author.idLong)
        val strippedUrl = url.removePrefix("<").removeSuffix(">")

        val playlist = if (playlistName !in allPlaylists && checkPlaylistCount(ctx, allPlaylists.size)) {
            Database.createPlaylist(ctx.author.idLong, playlistName)
                ?: return ctx.embed("Custom Playlists (Import)", "Failed to create a new playlist!")
        } else {
            Database.getPlaylist(ctx.author.idLong, playlistName)!!
        }

        if (Limits.customPlaylistTracks(ctx) - playlist.tracks.size <= 0) {
            return ctx.embed("Custom Playlists (Import)", "Your playlist is at maximum capacity!\nRemove some tracks and try again.")
        }

        ctx.asSlashContext?.deferAsync()

        val loaded = Launcher.playerManager.loadAsync(strippedUrl)
            ?: return ctx.embed("Custom Playlists (Import)", "The URL provided was invalid (no playlist found).")

        if (loaded !is AudioPlaylist) {
            return ctx.embed("Custom Playlists (Import)", "You can only import playlists (URL was not a playlist).")
        }

        // Some people might try and load again whilst another request is processing.
        val remainingTrackCapacity = Limits.customPlaylistTracks(ctx) - playlist.tracks.size

        if (remainingTrackCapacity <= 0) {
            return ctx.embed("Custom Playlists (Import)", "Your playlist is at maximum capacity!\nRemove some tracks and try again.")
        }

        playlist.tracks.addAll(loaded.tracks.take(remainingTrackCapacity))
        playlist.save()

        ctx.embed("Custom Playlists (Import)", "Added ${loaded.tracks.size.coerceAtMost(remainingTrackCapacity)} tracks to `$playlistName`!")
    }

    @SubCommand(aliases = ["remove", "rm", "del"], description = "Delete one of your custom playlists.")
    fun delete(ctx: Context,
               @Autocomplete("autocompletePlaylistName")
               @Describe("The name of the playlist to delete.")
               @Range(string = [1, 32])
               @Greedy playlistName: String) {
        if (Database.getPlaylist(ctx.author.idLong, playlistName) == null) {
            return ctx.embed("Custom Playlists (Delete)", "A playlist with that name wasn't found.\nYou can check your playlists with `/playlists list`")
        }

        Database.deletePlaylist(ctx.author.idLong, playlistName)
        ctx.embed("Custom Playlists (Delete)", "Your playlist has been deleted.")
    }

    @SubCommand(aliases = ["play", "p"], description = "Play a custom playlist of yours.")
    @TriggerConnect
    suspend fun load(ctx: Context,
                     @Autocomplete("autocompletePlaylistName")
                     @Describe("The name of the playlist to load.")
                     @Range(string = [1, 32])
                     @Greedy playlistName: String) {
        if (!ctx.isFromGuild) {
            return ctx.embed("Server Only", "This command can only be used from within a server.")
        }

        val playlist = Database.getPlaylist(ctx.author.idLong, playlistName)
            ?: return ctx.embed("Custom Playlists (Load)", "A playlist with that name wasn't found.\nYou can check your playlists with `/playlists list`")

        if (playlist.tracks.isEmpty()) {
            return ctx.embed("Custom Playlists (Load)", "That playlist doesn't have any tracks. Why not add some?")
        }

        ctx.asSlashContext?.deferAsync()

        val player = ctx.audioPlayer()

        for (track in playlist.tracks) {
            player.enqueue(track, ctx.author.idLong, false)
        }

        ctx.embed("Custom Playlists (Load)", "Loaded ${playlist.tracks.size} tracks from playlist `${playlist.title}`.")
    }

    private fun checkPlaylistCount(ctx: Context, count: Int): Boolean {
        val limit = Limits.customPlaylists(ctx)

        if (limit > count) {
            return true
        }

        val response = buildString {
            appendLine("You've reached the maximum amount of custom playlists.")

            if (Limits.MAX_TIER > ctx.premiumTier) { // can't upgrade any further lol
                append("[Upgrade your tier](https://patreon.com/devoxin) to get more slots!")
            }
        }

        ctx.embed("Custom Playlists", response)
        return false
    }

    fun autocompletePlaylistName(event: CommandAutoCompleteInteractionEvent) {
        val playlists = Database.getPlaylists(event.user.idLong)

        if (playlists.isEmpty()) {
            return event.replyChoices().queue()
        }

        val value = event.focusedOption.value
        val filtered = playlists.filter { StringUtils.isSubstringWithin(it, value) }
            .map { Choice(it, it) }

        event.replyChoices(filtered).queue()
    }

    // view
    // manage
    // delete
    // load
}
