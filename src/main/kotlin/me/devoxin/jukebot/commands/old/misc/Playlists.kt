//package me.devoxin.jukebot.commands.old.misc
//
//import com.sedmelluq.discord.lavaplayer.player.FunctionalResultHandler
//import me.devoxin.flight.api.annotations.SubCommand
//import me.devoxin.jukebot.Database
//import me.devoxin.jukebot.JukeBot
//import me.devoxin.jukebot.Launcher
//import me.devoxin.jukebot.models.CustomPlaylist
//import me.devoxin.jukebot.annotations.*
//import me.devoxin.jukebot.utils.*
//import net.dv8tion.jda.api.EmbedBuilder
//import net.dv8tion.jda.api.entities.Message
//import net.dv8tion.jda.api.entities.MessageEmbed
//import net.dv8tion.jda.api.utils.messages.MessageEditData
//import kotlin.math.ceil
//
//@CommandProperties(description = "Manage personal playlists stored within the bot.", aliases = ["pl", "playlist"])
//class Playlists : Command(ExecutionType.STANDARD) {
//    @SubCommand(trigger = "view", description = "Lists the tracks in a playlist.")
//    fun viewPlaylist(ctx: Context) {
//        val playlistName = ctx.args.next("playlist_name", ArgumentResolver.STRING)
//            ?: return ctx.embed("Custom Playlists", "You need to provide the name of the playlist to view.")
//
//        val playlist = Database.getPlaylist(ctx.author.idLong, playlistName)
//            ?: return ctx.embed("Custom Playlists", "That playlist doesn't exist.")
//
//        val trackList = if (playlist.tracks.isEmpty()) {
//            "`No tracks.`"
//        } else {
//            val append = if (playlist.tracks.size > 10) "\n*...and ${playlist.tracks.size - 10} more tracks.*" else ""
//            Page.paginate(playlist.tracks, 1).content + append
//        }
//
//        ctx.embed("Custom Playlists - ${playlist.title}", trackList)
//    }
//
//    @SubCommand(trigger = "manage", description = "Make modifications to a playlist.")
//    fun manage(ctx: Context) {
//        val playlistName = ctx.args.gatherNext("playlist_name").takeIf { it.isNotEmpty() }
//            ?: return ctx.embed("Custom Playlists", "You need to provide the name of the playlist to load.")
//
//        val playlist = Database.getPlaylist(ctx.author.idLong, playlistName)
//            ?: return ctx.embed("Custom Playlists", "That playlist doesn't exist.")
//
//        if (playlist.tracks.isEmpty()) {
//            return ctx.embed("Custom Playlists", "No tracks to manage.")
//        }
//
//        val em = buildEmbed(ctx, playlist).toMessage()
//        ctx.channel.sendMessage(em).queue {
//            manageMenu(ctx, it, playlist, 1)
//        }
//    }
//
//    fun manageMenu(ctx: Context, dialog: Message, playlist: CustomPlaylist, page: Int) {
//        ctx.prompt(30) { r ->
//            val (cmd, args) = r?.split("\\s+".toRegex())?.separate() ?: return@prompt
//
//            when (cmd) {
//                "help" -> {
//                    ctx.embed(
//                        "Managing Playlist - ${playlist.title}",
//                        "**`remove:`** Removes the track at the given index.\n" +
//                            "**`move \u200B \u200B:`** Moves a track to the specified index.\n" +
//                            "**`page \u200B \u200B:`** Displays a different page.\n" +
//                            "**`save \u200B \u200B:`** Saves any changes you've made to the database."
//                    )
//                    // u200B is a magical hack that allows our embeds to keep their formatting
//                    // Don't tell Discord, though :>
//                    manageMenu(ctx, dialog, playlist, page)
//                }
//                "remove" -> {
//                    val index = args.firstOrNull()?.toIntOrNull()?.takeIf { it in 1..playlist.tracks.size }
//
//                    if (index == null) {
//                        ctx.embed(
//                            "Managing Playlist - ${playlist.title}",
//                            "Index needs be higher than 0, and equal to or less than ${playlist.tracks.size}."
//                        )
//                        return@prompt manageMenu(ctx, dialog, playlist, page)
//                    }
//
//                    playlist.tracks.removeAt(index - 1)
//                    dialog.editMessage(MessageEditData.fromEmbeds(buildEmbed(ctx, playlist, page)))
//                        .queue { manageMenu(ctx, it, playlist, page) }
//                }
//                "page" -> {
//                    val maxPages = ceil(playlist.tracks.size.toDouble() / 10).toInt()
//                    val index = args.firstOrNull()?.toIntOrNull()?.takeIf { it in 1..maxPages }
//
//                    if (index == null) {
//                        ctx.embed(
//                            "Managing Playlist - ${playlist.title}",
//                            "Page needs be higher than 0, and equal to or less than $maxPages."
//                        )
//                        return@prompt manageMenu(ctx, dialog, playlist, page)
//                    }
//
//                    dialog.editMessage(MessageEditData.fromEmbeds(buildEmbed(ctx, playlist, index)))
//                        .queue { manageMenu(ctx, it, playlist, index) }
//                }
//                "move" -> {
//                    if (args.size < 2) {
//                        ctx.embed(
//                            "Managing Playlist - ${playlist.title}",
//                            "You need to specify two numbers, higher than 0 and equal to or less than ${playlist.tracks.size}"
//                        )
//                        return@prompt manageMenu(ctx, dialog, playlist, page)
//                    }
//
//                    val i1 = args[0].toIntOrNull()?.takeIf { it in 1..playlist.tracks.size }
//                    val i2 = args[1].toIntOrNull()?.takeIf { it in 1..playlist.tracks.size && it != i1 }
//
//                    if (i1 == null || i2 == null) {
//                        ctx.embed(
//                            "Managing Playlist - ${playlist.title}",
//                            "You need to specify a valid target track, and a valid target position."
//                        )
//                        return@prompt manageMenu(ctx, dialog, playlist, page)
//                    }
//
//                    val selectedTrack = playlist.tracks[i1 - 1]
//                    playlist.tracks.removeAt(i1 - 1)
//                    playlist.tracks.add(i2 - 1, selectedTrack)
//
//                    dialog.editMessage(MessageEditData.fromEmbeds(buildEmbed(ctx, playlist, page)))
//                        .queue { manageMenu(ctx, it, playlist, page) }
//                }
//                "save" -> {
//                    playlist.save()
//                    ctx.embed("Managing Playlist - ${playlist.title}", "Playlist saved.")
//                }
//            }
//        }
//    }
//
//    private fun buildEmbed(ctx: Context, playlist: CustomPlaylist, selectedPage: Int = 1): MessageEmbed {
//        val page = Page.paginate(playlist.tracks, selectedPage)
//
//        return EmbedBuilder()
//            .setColor(ctx.embedColor)
//            .setTitle("Managing Playlist - ${playlist.title}")
//            .setDescription(page.content)
//            .setFooter("Duration: ${page.duration} • Page ${page.page}/${page.maxPages} • Send \"help\" for management commands")
//            .build()
//    }
//}
