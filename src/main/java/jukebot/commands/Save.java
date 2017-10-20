package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.JukeBot;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;

public class Save implements Command {

    public void execute(GuildMessageReceivedEvent e, String query) {

        final GuildMusicManager musicManager = JukeBot.getGuildMusicManager(e.getGuild().getAudioManager());
        final AudioTrack currentTrack = musicManager.player.getPlayingTrack();

        if (currentTrack == null) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("No playback activity")
                    .setDescription("There's nothing playing.")
                    .build()
            ).queue();
            return;
        }

        if (query.length() > 0 && "all".equalsIgnoreCase(query)) {
            List<AudioTrack> queue = musicManager.handler.getQueue();

            if (queue.size() == 0) {
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("No songs queued")
                        .setDescription("There are no songs in the queue.")
                        .build()
                ).queue();
                return;
            }

            StringBuilder sb = new StringBuilder();

            for (AudioTrack track : queue)
                sb.append(track.getInfo().title)
                        .append(" - ")
                        .append(track.getInfo().uri)
                        .append("\r\n");

            e.getAuthor().openPrivateChannel().queue(dm ->
                    dm.sendFile(
                            sb.toString().getBytes(), "queue.txt", null
                    ).queue(null, error ->
                            e.getChannel().sendMessage(new EmbedBuilder()
                                    .setColor(Bot.EmbedColour)
                                    .setTitle("Unable to DM")
                                    .setDescription("I was unable to DM you.\nEnsure I'm not blocked and your DMs are enabled.")
                                    .build()
                            ).queue()
                    )
            );
        } else {
            e.getAuthor().openPrivateChannel().queue(dm ->
                    dm.sendMessage(
                            new EmbedBuilder()
                            .setColor(Bot.EmbedColour)
                            .setTitle(currentTrack.getInfo().title, currentTrack.getInfo().uri)
                            .build()
                    ).queue(null, error ->
                            e.getChannel().sendMessage(new EmbedBuilder()
                                    .setColor(Bot.EmbedColour)
                                    .setTitle("Unable to DM")
                                    .setDescription("I was unable to DM you.\nEnsure I'm not blocked and your DMs are enabled.")
                                    .build()
                            ).queue()
                    )
            );
        }

    }
}
