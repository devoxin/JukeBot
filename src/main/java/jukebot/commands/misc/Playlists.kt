package jukebot.commands.misc

import com.sedmelluq.discord.lavaplayer.player.FunctionalResultHandler
import jukebot.Database
import jukebot.JukeBot
import jukebot.entities.CustomPlaylist
import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context
import jukebot.framework.SubCommand
import jukebot.utils.Helpers
import jukebot.utils.Page
import jukebot.utils.separate
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import java.util.function.Consumer
import kotlin.math.ceil

@CommandProperties(description = "Manage personal playlists stored within the bot.", aliases = ["pl", "playlist"])
class Playlists : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        val sc = context.args.firstOrNull() ?: ""

        if (!this.subcommands.containsKey(sc)) {
            return context.embed(
                "Custom Playlists",
                this.subcommands.map { "**`${Helpers.pad(it.key)}:`** ${it.value.description}" }.joinToString("\n")
            )
        }

        this.subcommands[sc]!!.invoke(context, withArgs = true)
    }

    @SubCommand(trigger = "list", description = "View your custom playlists.")
    fun listPlaylists(ctx: Context, args: List<String>) {
        val userPlaylists = Database.getPlaylists(ctx.author.idLong)

        if (userPlaylists.isEmpty()) {
            return ctx.embed("Custom Playlists", "You don't have any playlists :(")
        }

        ctx.embed("Custom Playlists", userPlaylists.joinToString("\n"))
    }

    @SubCommand(trigger = "create", description = "Create a new custom playlist.")
    fun createPlaylist(ctx: Context, args: List<String>) {
        val allPlaylists = Database.getPlaylists(ctx.author.idLong)

        if (!checkPlaylistCount(ctx, allPlaylists.size)) {
            return
        }

        if (args.isEmpty()) {
            ctx.prompt("Custom Playlists", "What do you want to name the playlist?\n*Max. 32 characters*") { _, title ->
                if (title == null) {
                    return@prompt ctx.embed("Custom Playlists", "Playlist creation cancelled.")
                }

                createPlaylistWithTitle(ctx, title)
            }
        } else {
            createPlaylistWithTitle(ctx, args[0])
        }
    }

    @SubCommand(trigger = "import", description = "Import a playlist from YouTube/SoundCloud/etc.")
    fun importPlaylist(ctx: Context, args: List<String>) {
        val allPlaylists = Database.getPlaylists(ctx.author.idLong)

        if (!checkPlaylistCount(ctx, allPlaylists.size)) {
            return
        }

        if (args.size < 2) {
            return ctx.embed("Custom Playlists", "You need to specify a URL, and title.\n" +
                "The URL should point to the playlist you want to imported.\n" +
                "Title can be anything, however it must be no more than one word.\n" +
                "`${ctx.prefix}playlists import <url> <title>`")
        }

        val title = args[1]
        val handler = FunctionalResultHandler(
            Consumer { ctx.embed("Import Playlist", "You need to provide a playlist URL.\nYou provided a track URL.") },
            Consumer {
                val imported = it.tracks.take(CustomPlaylist.TRACK_LIMIT)
                ctx.embed("Import Playlist", "Importing **${imported.size}** tracks from **${it.name}**...")

                Database.createPlaylist(ctx.author.idLong, title)
                val playlist = Database.getPlaylist(ctx.author.idLong, title)
                    ?: return@Consumer ctx.embed("Import Playlist", "An unknown error occurred while creating the playlist.")

                playlist.tracks.addAll(imported)
                playlist.save()

                ctx.embed("Import Playliss", "Playlist imported as **$title** successfully.")
            },
            Runnable { ctx.embed("Import Playlist", "No results found!") },
            Consumer { ctx.embed("Import Playlist", "An error occurred while loading the URL.") }
        )

        JukeBot.playerManager.loadItem(args[0], handler)
    }

    @SubCommand(trigger = "view", description = "Lists the tracks in a playlist.")
    fun viewPlaylist(ctx: Context, args: List<String>) {
        val playlistName = args.joinToString(" ")

        if (playlistName.isEmpty()) {
            return ctx.embed("Custom Playlists", "You need to provide the name of the playlist to view.")
        }

        val playlist = Database.getPlaylist(ctx.author.idLong, playlistName)
            ?: return ctx.embed("Custom Playlists", "That playlist doesn't exist.")

        val trackList = if (playlist.tracks.isEmpty()) {
            "`No tracks.`"
        } else {
            val append = if (playlist.tracks.size > 10) "\n*...and ${playlist.tracks.size - 10} more tracks.*" else ""
            Page.paginate(playlist.tracks, 1).content + append
        }

        ctx.embed("Custom Playlists - ${playlist.title}", trackList)
    }

    @SubCommand(trigger = "manage", description = "Make modifications to a playlist.")
    fun manage(ctx: Context, args: List<String>) {
        val playlistName = args.joinToString(" ").takeIf { it.isNotEmpty() }
            ?: return ctx.embed("Custom Playlists", "You need to provide the name of the playlist to load.")

        val playlist = Database.getPlaylist(ctx.author.idLong, playlistName)
            ?: return ctx.embed("Custom Playlists", "That playlist doesn't exist.")

        if (playlist.tracks.isEmpty()) {
            return ctx.embed("Custom Playlists", "No tracks to manage.")
        }

        val em = buildEmbed(ctx, playlist)
        ctx.channel.sendMessage(em).queue {
            manageMenu(ctx, it, playlist, 1)
        }
    }

    fun manageMenu(ctx: Context, dialog: Message, playlist: CustomPlaylist, page: Int) {
        ctx.prompt(30) { r ->
            val (cmd, args) = r?.split("\\s+".toRegex())?.separate() ?: return@prompt

            when (cmd) {
                "help" -> {
                    ctx.embed(
                        "Managing Playlist - ${playlist.title}",
                        "**`remove:`** Removes the track at the given index.\n" +
                            "**`move \u200B \u200B:`** Moves a track to the specified index.\n" +
                            "**`page \u200B \u200B:`** Displays a different page.\n" +
                            "**`save \u200B \u200B:`** Saves any changes you've made to the database.")
                    // u200B is a magical hack that allows our embeds to keep their formatting
                    // Don't tell Discord, though :>
                    manageMenu(ctx, dialog, playlist, page)
                }
                "remove" -> {
                    val index = args.firstOrNull()?.toIntOrNull()?.takeIf { it in 1..playlist.tracks.size }

                    if (index == null) {
                        ctx.embed("Managing Playlist - ${playlist.title}", "Index needs be higher than 0, and equal to or less than ${playlist.tracks.size}.")
                        return@prompt manageMenu(ctx, dialog, playlist, page)
                    }

                    playlist.tracks.removeAt(index - 1)
                    dialog.editMessage(buildEmbed(ctx, playlist, page)).queue { manageMenu(ctx, it, playlist, page) }
                }
                "page" -> {
                    val maxPages = ceil(playlist.tracks.size.toDouble() / 10).toInt()
                    val index = args.firstOrNull()?.toIntOrNull()?.takeIf { it in 1..maxPages }

                    if (index == null) {
                        ctx.embed("Managing Playlist - ${playlist.title}", "Page needs be higher than 0, and equal to or less than $maxPages.")
                        return@prompt manageMenu(ctx, dialog, playlist, page)
                    }

                    dialog.editMessage(buildEmbed(ctx, playlist, index)).queue { manageMenu(ctx, it, playlist, index) }
                }
                "move" -> {
                    if (args.size < 2) {
                        ctx.embed("Managing Playlist - ${playlist.title}", "You need to specify two numbers, higher than 0 and equal to or less than ${playlist.tracks.size}")
                        return@prompt manageMenu(ctx, dialog, playlist, page)
                    }

                    val i1 = args[0].toIntOrNull()?.takeIf { it in 1..playlist.tracks.size }
                    val i2 = args[1].toIntOrNull()?.takeIf { it in 1..playlist.tracks.size && it != i1 }

                    if (i1 == null || i2 == null) {
                        ctx.embed("Managing Playlist - ${playlist.title}", "You need to specify a valid target track, and a valid target position.")
                        return@prompt manageMenu(ctx, dialog, playlist, page)
                    }

                    val selectedTrack = playlist.tracks[i1 - 1]
                    playlist.tracks.removeAt(i1 - 1)
                    playlist.tracks.add(i2 - 1, selectedTrack)

                    dialog.editMessage(buildEmbed(ctx, playlist, page)).queue { manageMenu(ctx, it, playlist, page) }
                }
                "save" -> {
                    playlist.save()
                    ctx.embed("Managing Playlist - ${playlist.title}", "Playlist saved.")
                }
            }
        }
    }

    @SubCommand(trigger = "delete", description = "Deletes a custom playlist.")
    fun delete(ctx: Context, args: List<String>) {
        val playlistName = args.joinToString(" ").takeIf { it.isNotEmpty() }
            ?: return ctx.embed("Custom Playlists", "You need to provide the name of the playlist to delete.")

        Database.getPlaylist(ctx.author.idLong, playlistName)
            ?: return ctx.embed("Custom Playlists", "That playlist doesn't exist.")

        Database.deletePlaylist(ctx.author.idLong, playlistName)
        ctx.embed("Custom Playlists", "Playlist deleted.")
    }

    @SubCommand(trigger = "load", description = "Loads a playlist into the queue.")
    fun load(ctx: Context, args: List<String>) {
        val playlistName = args.joinToString(" ").takeIf { it.isNotEmpty() }
            ?: return ctx.embed("Custom Playlists", "You need to provide the name of the playlist to load.")

        val playlist = Database.getPlaylist(ctx.author.idLong, playlistName)
            ?: return ctx.embed("Custom Playlists", "That playlist doesn't exist.")

        if (!this.connectToChannel(ctx)) {
            return
        }

        val player = ctx.getAudioPlayer()

        if (!player.isPlaying) {
            player.channelId = ctx.channel.idLong
        }

        for (track in playlist.tracks) {
            player.enqueue(track, ctx.author.idLong, false)
        }

        ctx.embed("Custom Playlists", "Loaded `${playlist.tracks.size}` tracks from playlist `${playlist.title}`")
    }

    private fun createPlaylistWithTitle(ctx: Context, title: String) {
        if (title.length > 32) {
            return ctx.embed("Custom Playlists", "The playlist name cannot be longer than 32 characters!")
        }

        Database.createPlaylist(ctx.author.idLong, title)

        ctx.embed("Custom Playlists", ":fire: Any time you hear a song you like, you can add it to your new playlist " +
            "by running `${ctx.prefix}save $title`")
    }

    private fun buildEmbed(ctx: Context, playlist: CustomPlaylist, selectedPage: Int = 1): MessageEmbed {
        val page = Page.paginate(playlist.tracks, selectedPage)

        return EmbedBuilder()
            .setColor(ctx.embedColor)
            .setTitle("Managing Playlist - ${playlist.title}")
            .setDescription(page.content)
            .setFooter("Duration: ${page.duration} • Page ${page.page}/${page.maxPages} • Send \"help\" for management commands")
            .build()
    }

    private fun checkPlaylistCount(ctx: Context, count: Int): Boolean {
        if (JukeBot.isSelfHosted || ctx.author.idLong == JukeBot.botOwnerId) {
            return true
        }

        val donorTier = Database.getTier(ctx.author.idLong)
        val cap = when {
            donorTier < 1 -> 5
            donorTier < 2 -> 50
            else -> 100
        }

        if (count < cap) {
            return true
        }

        val response = buildString {
            appendln("You've reached the maximum amount of custom playlists.")

            if (count < 100) { // Hit 5/50 cap
                append("[Upgrade your tier](https://patreon.com/devoxin) to get more slots!")
            }
        }

        ctx.embed("Custom Playlists", response)
        return false
    }

}
