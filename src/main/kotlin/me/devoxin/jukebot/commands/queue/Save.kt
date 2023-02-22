package me.devoxin.jukebot.commands.queue

import me.devoxin.jukebot.Database
import me.devoxin.jukebot.framework.*
import me.devoxin.jukebot.utils.Limits

@CommandProperties(description = "Adds the current track to a custom playlist", category = CommandCategory.QUEUE)
@CommandChecks.Playing
class Save : Command(ExecutionType.STANDARD) {
    override fun execute(context: Context) {
        val player = context.audioPlayer
        val currentTrack = player.player.playingTrack

        if (context.args.isEmpty()) {
            return context.embed("Save", "You need to provide the name of the playlist to add the track to.")
        }

        val playlist = Database.getPlaylist(context.author.idLong, context.argString)
            ?: return context.embed("Save", "That playlist doesn't exist.")

        if (playlist.tracks.size >= Limits.CUSTOM_PLAYLIST_MAX_TRACKS) {
            return context.embed("Save", "You've hit the maximum amount of tracks for this playlist! (${Limits.CUSTOM_PLAYLIST_MAX_TRACKS})")
        }

        playlist.tracks.add(currentTrack.makeClone())
        playlist.save()

        context.embed("Save", "Saved `${currentTrack.info.title}` to playlist `${playlist.title}`.")
    }
}