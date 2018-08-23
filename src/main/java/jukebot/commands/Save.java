package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;
import net.dv8tion.jda.core.EmbedBuilder;

@CommandProperties(description = "DMs you the currently playing track. Specify `all` to save the queue", category = CommandProperties.category.MEDIA)
public class Save implements Command {

    public void execute(final Context context) {

        final AudioHandler player = context.getAudioPlayer();
        final AudioTrack currentTrack = player.player.getPlayingTrack();

        if (!player.isPlaying()) {
            context.sendEmbed("Not Playing", "Nothing is currently playing.");
            return;
        }

        if ("all".equalsIgnoreCase(context.getArgString())) {

            if (player.getQueue().isEmpty()) {
                context.sendEmbed("Queue is empty", "There are no tracks to save.");
                return;
            }

            StringBuilder sb = new StringBuilder();

            for (AudioTrack track : player.getQueue())
                sb.append(track.getInfo().title)
                        .append(" - ")
                        .append(track.getInfo().uri)
                        .append("\r\n");

            context.getAuthor().openPrivateChannel().queue(dm ->
                    dm.sendFile(
                            sb.toString().getBytes(), "queue.txt", null
                    ).queue(null, error ->
                            context.sendEmbed("Unable to DM", "Ensure your DMs are enabled.")
                    )
            );
        } else {
            context.getAuthor().openPrivateChannel().queue(dm ->
                    dm.sendMessage(
                            new EmbedBuilder()
                                    .setColor(JukeBot.embedColour)
                                    .setTitle(currentTrack.getInfo().title, currentTrack.getInfo().uri)
                                    .build()
                    ).queue(null, error ->
                            context.sendEmbed("Unable to DM", "Ensure your DMs are enabled.")
                    )
            );
        }

    }
}
