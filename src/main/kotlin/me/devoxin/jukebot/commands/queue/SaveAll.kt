package me.devoxin.jukebot.commands.queue

import me.devoxin.jukebot.Database
import me.devoxin.jukebot.framework.*
import me.devoxin.jukebot.utils.Limits

@CommandProperties(description = "Adds all tracks in the queue to a custom playlist", category = CommandCategory.QUEUE)
@CommandChecks.Playing
class SaveAll : Command(ExecutionType.STANDARD) {
    override fun execute(context: Context) {
        val player = context.audioPlayer
        val playlistName = context.args.gatherNext("playlist_name").takeIf { it.isNotEmpty() }
            ?: return context.embed("Save", "You need to provide the name of the playlist to add the track to.")

        val playlist = Database.getPlaylist(context.author.idLong, playlistName)
            ?: return context.embed("Save", "That playlist doesn't exist.")

        if (playlist.tracks.size >= Limits.CUSTOM_PLAYLIST_MAX_TRACKS) {
            return context.embed(
                "Save",
                "You've hit the maximum amount of tracks for this playlist! (${Limits.CUSTOM_PLAYLIST_MAX_TRACKS})"
            )
        }

        val tracksToAdd = player.queue
            .take(Limits.CUSTOM_PLAYLIST_MAX_TRACKS - playlist.tracks.size)
            .map { it.makeClone() }

        playlist.tracks.addAll(tracksToAdd)
        playlist.save()

        context.embed("Save", "Saved `${tracksToAdd.size}` tracks to playlist `${playlist.title}`.")
    }
}
