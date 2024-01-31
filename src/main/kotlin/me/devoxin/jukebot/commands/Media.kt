package me.devoxin.jukebot.commands

import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Describe
import me.devoxin.flight.api.annotations.Greedy
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.entities.Cog
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.Launcher
import me.devoxin.jukebot.annotations.Checks.Playing
import me.devoxin.jukebot.audio.sources.spotify.SpotifyAudioTrack
import me.devoxin.jukebot.extensions.audioPlayer
import me.devoxin.jukebot.extensions.createProgressBar
import me.devoxin.jukebot.extensions.embed
import me.devoxin.jukebot.extensions.toTimeString
import me.devoxin.jukebot.utils.Limits

class Media : Cog {
    @Command(aliases = ["n", "np", "now", "current"], description = "View the currently playing track.", guildOnly = true)
    @Playing
    fun nowplaying(ctx: Context) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        val track = player.player.playingTrack
            ?: return ctx.embed("Not Playing", "There's nothing playing.")

        val duration = track.takeIf { !it.info.isStream }?.duration?.toTimeString() ?: "∞"
        val requester = if (track.userData as Long == Launcher.shardManager.botId) "AutoPlay" else "<@${track.userData}>"

        ctx.embed {
            setDescription("**${track.info.title}**\n*${track.info.author} - $duration*\n$requester\n${track.position.toTimeString()} ${track.position.createProgressBar(track.duration, 10, track.info.uri)} $duration")

            if (track is SpotifyAudioTrack) {
                setThumbnail(track.artworkUrl)
            }
        }
    }

    @Command(description = "Adds the current track to a custom playlist.", guildOnly = true)
    @Playing
    fun save(ctx: Context,
             @Describe("The name of the playlist to save the track to.") @Greedy playlistName: String) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        val track = player.player.playingTrack
            ?: return ctx.embed("Not Playing", "There's nothing playing.")

        val playlist = Database.getPlaylist(ctx.author.idLong, playlistName)
            ?: return ctx.embed("Save Track", "That playlist doesn't exist.")

        val limit = Limits.customPlaylistTracks(ctx)
        val remainingSlots = limit - playlist.tracks.size

        if (remainingSlots <= 0) {
            return ctx.embed("Save Track", "Your playlist is at maximum capacity! (${playlist.tracks.size}/$limit)")
        }

        playlist.tracks.add(track.makeClone())
        playlist.save()

        ctx.embed("Track Saved", "Saved `${track.info.title}` to playlist `${playlist.title}`.")
    }

    @Command(description = "Adds all tracks in the queue to a custom playlist.", guildOnly = true)
    @Playing
    fun saveAll(ctx: Context,
                @Describe("The name of the playlist to save the tracks to.") @Greedy playlistName: String) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        if (player.queue.isEmpty()) {
            return ctx.embed("Save Tracks", "There's nothing to save as the queue is empty.")
        }

        val playlist = Database.getPlaylist(ctx.author.idLong, playlistName)
            ?: return ctx.embed("Save Tracks", "That playlist doesn't exist.")

        val limit = Limits.customPlaylistTracks(ctx)
        val remainingSlots = limit - playlist.tracks.size

        if (remainingSlots <= 0) {
            return ctx.embed("Save Tracks", "Your playlist is at maximum capacity! (${playlist.tracks.size}/$limit)")
        }

        val tracksToAdd = player.queue
            .take(remainingSlots)
            .map { it.makeClone() }

        playlist.tracks.addAll(tracksToAdd)
        playlist.save()

        ctx.embed("Tracks Saved", "Saved `${tracksToAdd.size}` tracks to playlist `${playlist.title}`.")
    }
}
