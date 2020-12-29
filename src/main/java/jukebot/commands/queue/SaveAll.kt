package jukebot.commands.queue

import jukebot.Database
import jukebot.entities.CustomPlaylist
import jukebot.framework.*

@CommandProperties(description = "Adds all tracks in the queue to a custom playlist", category = CommandCategory.QUEUE)
@CommandChecks.Playing
class SaveAll : Command(ExecutionType.STANDARD) {
    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        if (context.args.isEmpty()) {
            return context.embed("Save", "You need to provide the name of the playlist to add the track to.")
        }

        val playlist = Database.getPlaylist(context.author.idLong, context.argString)
            ?: return context.embed("Save", "That playlist doesn't exist.")

        if (playlist.tracks.size >= CustomPlaylist.TRACK_LIMIT) {
            return context.embed("Save", "You've hit the maximum amount of tracks for this playlist! (${CustomPlaylist.TRACK_LIMIT})")
        }

        val tracksToAdd = player.queue
            .take(CustomPlaylist.TRACK_LIMIT - playlist.tracks.size)
            .map { it.makeClone() }

        playlist.tracks.addAll(tracksToAdd)
        playlist.save()

        context.embed("Save", "Saved `${tracksToAdd.size}` tracks to playlist `${playlist.title}`.")
    }
}
