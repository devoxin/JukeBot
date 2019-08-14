package jukebot.commands

import jukebot.Database
import jukebot.framework.Command
import jukebot.framework.CommandCategory
import jukebot.framework.CommandProperties
import jukebot.framework.Context
import net.dv8tion.jda.api.EmbedBuilder

@CommandProperties(description = "DMs you the currently playing track. Specify `all` to save the queue", category = CommandCategory.QUEUE)
class Save : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()
        val currentTrack = player.player.playingTrack

        if (!player.isPlaying) {
            return context.embed("Not Playing", "Nothing is currently playing.")
        }

        if (context.argString.isEmpty()) {
            return context.embed("Save", "You need to provide the name of the playlist to add the track to.")
        }

        val playlist = Database.getPlaylist(context.author.idLong, context.argString)
                ?: return context.embed("Save", "That playlist doesn't exist.")

        if (playlist.tracks.size >= 100) {
            return context.embed("Save", "You've hit the maximum amount of tracks for this playlist! (100)")
        }

        playlist.tracks.add(currentTrack.makeClone())
        playlist.save()

        context.embed("Save", "Saved `${currentTrack.info.title}` to playlist `${playlist.title}`.")
//        if ("all".equals(context.argString, ignoreCase = true)) {
//            if (player.queue.isEmpty()) {
//                return context.embed("Queue is empty", "There are no tracks to save.")
//            }
//
//            val queue = player.queue.joinToString("\r\n") { "${it.info.title} - ${it.info.uri}" }
//
//            context.author.openPrivateChannel().queue { dm ->
//                dm.sendFile(queue.toByteArray(), "queue.txt", null)
//                        .queue(null, { context.embed("Unable to DM", "Ensure your DMs are enabled.") })
//            }
//        } else {
//            context.author.openPrivateChannel().queue { dm ->
//                dm.sendMessage(
//                        EmbedBuilder()
//                                .setColor(context.embedColor)
//                                .setTitle(currentTrack.info.title, currentTrack.info.uri)
//                                .build())
//                        .queue(null, { context.embed("Unable to DM", "Ensure your DMs are enabled.") })
//            }
//        }
    }
}
