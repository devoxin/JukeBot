package jukebot.commands

import jukebot.Database
import jukebot.entities.CustomPlaylist
import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context
import jukebot.framework.SubCommand
import jukebot.utils.Page
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import java.util.regex.Pattern
import kotlin.math.ceil

@CommandProperties(description = "Manage personal playlists stored within the bot.", aliases = ["pl"])
class Playlists : Command(ExecutionType.STANDARD) {

    fun pad(s: String): String {
        return String.format("%-12s", s).replace(" ", " \u200B")
    }

    override fun execute(context: Context) {
        val sc = context.args.firstOrNull() ?: ""

        if (!this.subcommands.containsKey(sc)) {
            return context.embed(
                    "Custom Playlists",
                    this.subcommands.map { "**`${pad(it.key)}:`** ${it.value.description}" }.joinToString("\n")
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

        ctx.embed("Custom Playlists", userPlaylists.joinToString("\n") { it })
    }

    @SubCommand(trigger = "create", description = "Create a new custom playlist.")
    fun createPlaylist(ctx: Context) {
        ctx.prompt("Custom Playlists", "What do you want to name the playlist?\n*Max. 32 characters*") { m, title ->
            if (title == null) {
                return@prompt ctx.embed("Custom Playlists", "Playlist creation cancelled.")
            }

            if (title.length > 32) {
                return@prompt ctx.embed("Custom Playlists", "The playlist name cannot be longer than 32 characters!")
            }

            Database.createPlaylist(ctx.author.idLong, title)

            ctx.embed("Custom Playlists", ":fire: Any time you hear a song you like, you can add it to your new playlist " +
                    "by running `${ctx.prefix}save $title`")
        }
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
            playlist.tracks.take(10).joinToString("\n") { it.info.title } + append
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
            manage_func(ctx, it, playlist, 1)
        }
    }

    fun buildEmbed(ctx: Context, playlist: CustomPlaylist, selectedPage: Int = 1): MessageEmbed {
        val page = Page.paginate(playlist.tracks, selectedPage)

        return EmbedBuilder()
                .setColor(ctx.embedColor)
                .setTitle("Managing Playlist - ${playlist.title}")
                .setDescription(page.content)
                .setFooter("Duration: ${page.duration} • Page ${page.page}/${page.maxPages} • \"help\" for syntax")
                .build()
    }

    fun manage_func(ctx: Context, dialog: Message, playlist: CustomPlaylist, page: Int = 1) {
        ctx.prompt(30) {
            if (it == null) {
                return@prompt
            }

            val args = it.split(" +".toRegex())

            when(args.firstOrNull()) {
                "help" -> {
                    ctx.embed("Managing Playlist - ${playlist.title}", "help: Displays this\nremove <index>: Removes the track at the given index\nmove <i1> <i2>: Moves the track at i1 to i2\npage <index>: Displays the page at the given number\nsave: Saves the playlist to the database.")
                    manage_func(ctx, dialog, playlist, page)
                }
                "remove" -> {
                    val index = args.drop(1).firstOrNull()?.toIntOrNull()

                    if (index == null) {
                        ctx.embed("Managing Playlist - ${playlist.title}", "Index needs to be a number.")
                        return@prompt manage_func(ctx, dialog, playlist, page)
                    }

                    if (index <= 0 || index > playlist.tracks.size) {
                        ctx.embed("Managing Playlist - ${playlist.title}", "Index needs be higher than 0, and equal to or less than ${playlist.tracks.size}.")
                        return@prompt manage_func(ctx, dialog, playlist, page)
                    }

                    playlist.tracks.removeAt(index - 1)
                    dialog.editMessage(buildEmbed(ctx, playlist, page)).queue { nm ->
                        manage_func(ctx, nm, playlist, page)
                    }
                }
                "page" -> {
                    val index = args.drop(1).firstOrNull()?.toIntOrNull()

                    if (index == null) {
                        ctx.embed("Managing Playlist - ${playlist.title}", "Page needs to be a number.")
                        return@prompt manage_func(ctx, dialog, playlist, page)
                    }

                    val maxPages = ceil(playlist.tracks.size.toDouble() / 10).toInt()

                    if (index <= 0 || index > playlist.tracks.size) {
                        ctx.embed("Managing Playlist - ${playlist.title}", "Page needs be higher than 0, and equal to or less than $maxPages.")
                        return@prompt manage_func(ctx, dialog, playlist, page)
                    }

                    dialog.editMessage(buildEmbed(ctx, playlist, index)).queue { nm ->
                        manage_func(ctx, nm, playlist, index)
                    }
                }
                "move" -> {
                    val realArgs = args.drop(1)

                    if (realArgs.size < 2) {
                        ctx.embed("Managing Playlist - ${playlist.title}", "You need to specify `i1` and `i2`.")
                        return@prompt manage_func(ctx, dialog, playlist, page)
                    }

                    val i1 = realArgs[0].toIntOrNull()
                    val i2 = realArgs[1].toIntOrNull()

                    if (i1 == null || i2 == null || i1 < 1 || i2 < 1 || i1 == i2 || i1 > playlist.tracks.size || i2 > playlist.tracks.size) {
                        ctx.embed("Managing Playlist - ${playlist.title}", "You need to specify a valid target track, and a valid target position.")
                        return@prompt manage_func(ctx, dialog, playlist, page)
                    }

                    val selectedTrack = playlist.tracks[i1 - 1]
                    playlist.tracks.removeAt(i1 - 1)
                    playlist.tracks.add(i2 - 1, selectedTrack)

                    dialog.editMessage(buildEmbed(ctx, playlist, page)).queue { nm ->
                        manage_func(ctx, nm, playlist, page)
                    }
                }
                "save" -> {
                    playlist.save()
                    ctx.embed("Managing Playlist - ${playlist.title}", "Playlist saved.")
                }
            }
        }
    }

    @SubCommand(trigger = "load", description = "Loads a playlist into the queue.")
    fun load(ctx: Context) {
        val playlistName = ctx.args.drop(1).joinToString(" ")

        if (playlistName.isEmpty()) {
            return ctx.embed("Custom Playlists", "You need to provide the name of the playlist to load.")
        }

        val playlist = Database.getPlaylist(ctx.author.idLong, playlistName)
                ?: return ctx.embed("Custom Playlists", "That playlist doesn't exist.")

        val player = ctx.getAudioPlayer()

        for (track in playlist.tracks) {
            player.enqueue(track, ctx.author.idLong, false)
        }

        ctx.embed("Custom Playlists", "Loaded `${playlist.tracks.size}` tracks from playlist `${playlist.title}`")
    }

    // proper loading system
    // playlist management

}