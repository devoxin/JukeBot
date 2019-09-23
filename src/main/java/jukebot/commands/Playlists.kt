package jukebot.commands

import jukebot.Database
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

        this.subcommands[sc]!!.invoke(context)
    }

    @SubCommand(trigger = "list", description = "View your custom playlists.")
    fun listPlaylists(ctx: Context) {
        val userPlaylists = Database.getPlaylists(ctx.author.idLong)

        if (userPlaylists.isEmpty()) {
            return ctx.embed("Custom Playlists", "You don't have any playlists :(")
        }

        ctx.embed("Custom Playlists", userPlaylists.joinToString("\n"))
    }

    @SubCommand(trigger = "create", description = "Create a new custom playlist.")
    fun createPlaylist(ctx: Context) {
        val allPlaylists = Database.getPlaylists(ctx.author.idLong)
        val donorTier = ctx.donorTier

        if (allPlaylists.size >= 50) {
            ctx.embed("Custom Playlists", "You've reached the maximum amount of playlists!")
            return
        } else if (allPlaylists.size >= 5 && donorTier < 1) {
            ctx.embed(
                "Custom Playlists",
                "You've reached the maximum amount of custom playlists!\n" +
                    "[Consider becoming a Patron](https://patreon.com/Devoxin) to get more!"
            )
            return
        }

        val args = ctx.args.drop(1)

        if (args.isEmpty()) {
            ctx.prompt("Custom Playlists", "What do you want to name the playlist?\n*Max. 32 characters*") { _, title ->
                if (title == null) {
                    return@prompt ctx.embed("Custom Playlists", "Playlist creation cancelled.")
                }

                createPlaylistWithTitle(ctx, title)
            }
        } else {
            createPlaylistWithTitle(ctx, args.first())
        }
    }

    private fun createPlaylistWithTitle(ctx: Context, title: String) {
        if (title.length > 32) {
            return ctx.embed("Custom Playlists", "The playlist name cannot be longer than 32 characters!")
        }

        Database.createPlaylist(ctx.author.idLong, title)

        ctx.embed("Custom Playlists", ":fire: Any time you hear a song you like, you can add it to your new playlist " +
            "by running `${ctx.prefix}save $title`")
    }

    @SubCommand(trigger = "view", description = "Lists the tracks in a playlist.")
    fun viewPlaylist(ctx: Context) {
        val playlistName = ctx.args.drop(1).joinToString(" ")

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
    fun manage(ctx: Context) {
        val playlistName = ctx.args.drop(1).joinToString(" ")

        if (playlistName.isEmpty()) {
            return ctx.embed("Custom Playlists", "You need to provide the name of the playlist to load.")
        }

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
                    val index = args.firstOrNull()?.toIntOrNull() ?: 0

                    if (index < 1 || index > playlist.tracks.size) {
                        ctx.embed("Managing Playlist - ${playlist.title}", "Index needs be higher than 0, and equal to or less than ${playlist.tracks.size}.")
                        return@prompt manageMenu(ctx, dialog, playlist, page)
                    }

                    playlist.tracks.removeAt(index - 1)
                    dialog.editMessage(buildEmbed(ctx, playlist, page)).queue { manageMenu(ctx, it, playlist, page) }
                }
                "page" -> {
                    val index = args.firstOrNull()?.toIntOrNull() ?: 0
                    val maxPages = ceil(playlist.tracks.size.toDouble() / 10).toInt()

                    if (index < 1 || index > maxPages) {
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

                    val i1 = args[0].toIntOrNull() ?: 0
                    val i2 = args[1].toIntOrNull() ?: 0

                    if (i1 < 1 || i2 < 1 || i1 == i2 || i1 > playlist.tracks.size || i2 > playlist.tracks.size) {
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
    fun delete(ctx: Context) {
        val playlistName = ctx.args.drop(1).joinToString(" ")

        if (playlistName.isEmpty()) {
            return ctx.embed("Custom Playlists", "You need to provide the name of the playlist to delete.")
        }

        Database.getPlaylist(ctx.author.idLong, playlistName)
            ?: return ctx.embed("Custom Playlists", "That playlist doesn't exist.")

        Database.deletePlaylist(ctx.author.idLong, playlistName)
        ctx.embed("Custom Playlists", "Playlist deleted.")
    }

    @SubCommand(trigger = "load", description = "Loads a playlist into the queue.")
    fun load(ctx: Context) {
        val playlistName = ctx.args.drop(1).joinToString(" ")

        if (playlistName.isEmpty()) {
            return ctx.embed("Custom Playlists", "You need to provide the name of the playlist to load.")
        }

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

    private fun buildEmbed(ctx: Context, playlist: CustomPlaylist, selectedPage: Int = 1): MessageEmbed {
        val page = Page.paginate(playlist.tracks, selectedPage)

        return EmbedBuilder()
            .setColor(ctx.embedColor)
            .setTitle("Managing Playlist - ${playlist.title}")
            .setDescription(page.content)
            .setFooter("Duration: ${page.duration} • Page ${page.page}/${page.maxPages} • Send \"help\" for management commands")
            .build()
    }

    // proper loading system

}