package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.Database;
import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Helpers;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.LinkedList;

@CommandProperties(aliases = {"q", "list", "songs"})
public class Queue implements Command {

    public void execute(GuildMessageReceivedEvent e, String query) {

        final AudioHandler handler = JukeBot.getMusicManager(e.getGuild().getAudioManager()).handler;
        final LinkedList<AudioTrack> queue = handler.getQueue();

        if (queue.isEmpty()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("No songs queued")
                    .setDescription("Use `" + Database.getPrefix(e.getGuild().getIdLong()) + "now` to view the current track.")
                    .build()
            ).queue();
            return;
        }

        final String queueDuration = Helpers.fTime(queue.stream().map(AudioTrack::getDuration).reduce(0L, (a, b) -> a + b));
        final StringBuilder fQueue = new StringBuilder();

        final int maxPages = (int) Math.ceil((double) queue.size() / 10);
        int page = Helpers.ParseNumber(query, 1);

        if (page < 1)
            page = 1;

        if (page > maxPages)
            page = maxPages;

        int begin = (page - 1) * 10;
        int end = (begin + 10) > queue.size() ? queue.size() : (begin + 10);

        for (int i = begin; i < end; i++) {
            final AudioTrack track = queue.get(i);
            fQueue.append("`")
                    .append(i + 1)
                    .append(".` **[")
                    .append(track.getInfo().title)
                    .append("](")
                    .append(track.getInfo().uri)
                    .append(")** <@")
                    .append(track.getUserData())
                    .append(">\n");
        }

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(JukeBot.EmbedColour)
                .setTitle("Queue (" + queue.size() + " songs, " + queueDuration + ")")
                .setDescription(fQueue.toString().trim())
                .addField("\u200B", "**Repeat:** " + handler.getStringifiedRepeat() +
                        " **/ Shuffle:** " + (handler.isShuffleEnabled() ? "On" : "Off"), true)
                .setFooter("Viewing page " + (page) + "/" + (maxPages), null)
                .build()
        ).queue();

    }
}
