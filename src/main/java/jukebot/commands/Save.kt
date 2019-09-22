package jukebot.commands

import jukebot.Database
import jukebot.entities.CustomPlaylist
import jukebot.framework.Command
import jukebot.framework.CommandCategory
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(description = "Adds the current track to a custom playlist", category = CommandCategory.QUEUE)
class Save : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()
        val currentTrack = player.player.playingTrack

        if (!player.isPlaying) {
            return context.embed("Not Playing", "Nothing is currently playing.")
        }

        if (context.args.isEmpty()) {
            return context.embed("Save", "You need to provide the name of the playlist to add the track to.")
        }

        val playlist = Database.getPlaylist(context.author.idLong, context.argString)
            ?: return context.embed("Save", "That playlist doesn't exist.")

        if (playlist.tracks.size >= CustomPlaylist.TRACK_LIMIT) {
            return context.embed("Save", "You've hit the maximum amount of tracks for this playlist! (${CustomPlaylist.TRACK_LIMIT})")
        }

        playlist.tracks.add(currentTrack.makeClone())
        playlist.save()

        context.embed("Save", "Saved `${currentTrack.info.title}` to playlist `${playlist.title}`.")
    }
}
