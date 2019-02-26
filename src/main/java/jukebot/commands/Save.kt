package jukebot.commands

import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import net.dv8tion.jda.core.EmbedBuilder

@CommandProperties(description = "DMs you the currently playing track. Specify `all` to save the queue", category = CommandProperties.category.MEDIA)
class Save : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {

        val player = context.getAudioPlayer()
        val currentTrack = player.player.playingTrack

        if (!player.isPlaying) {
            return context.embed("Not Playing", "Nothing is currently playing.")
        }

        if ("all".equals(context.argString, ignoreCase = true)) {

            if (player.queue.isEmpty()) {
                return context.embed("Queue is empty", "There are no tracks to save.")
            }

            val sb = StringBuilder()

            for (track in player.queue)
                sb.append(track.info.title)
                        .append(" - ")
                        .append(track.info.uri)
                        .append("\r\n")

            context.author.openPrivateChannel().queue { dm ->
                dm.sendFile(sb.toString().toByteArray(), "queue.txt", null)
                        .queue(null, { context.embed("Unable to DM", "Ensure your DMs are enabled.") }
                        )
            }
        } else {
            context.author.openPrivateChannel().queue { dm ->
                dm.sendMessage(
                        EmbedBuilder()
                                .setColor(context.embedColor)
                                .setTitle(currentTrack.info.title, currentTrack.info.uri)
                                .build())
                        .queue(null, { context.embed("Unable to DM", "Ensure your DMs are enabled.") }
                        )
            }
        }

    }
}
