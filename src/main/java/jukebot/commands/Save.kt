package jukebot.commands

import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context
import net.dv8tion.jda.api.EmbedBuilder

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

            val queue = player.queue.joinToString("\r\n") { "${it.info.title} - ${it.info.uri}" }

            context.author.openPrivateChannel().queue { dm ->
                dm.sendFile(queue.toByteArray(), "queue.txt", null)
                        .queue(null, { context.embed("Unable to DM", "Ensure your DMs are enabled.") })
            }
        } else {
            context.author.openPrivateChannel().queue { dm ->
                dm.sendMessage(
                        EmbedBuilder()
                                .setColor(context.embedColor)
                                .setTitle(currentTrack.info.title, currentTrack.info.uri)
                                .build())
                        .queue(null, { context.embed("Unable to DM", "Ensure your DMs are enabled.") })
            }
        }
    }
}
