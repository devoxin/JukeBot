package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;
import jukebot.utils.Helpers;

import java.util.LinkedList;

@CommandProperties(description = "Displays the current queue", aliases = {"q", "list", "songs"}, category = CommandProperties.category.MEDIA)
public class Queue implements Command {

    public void execute(final Context context) {

        final AudioHandler player = context.getAudioPlayer();
        final LinkedList<AudioTrack> queue = player.getQueue();

        if (queue.isEmpty()) {
            context.sendEmbed("Queue is empty", "There are no tracks to display.\nUse `" + context.getPrefix() + "now` to view current track.");
            return;
        }

        final String queueDuration = Helpers.Companion.fTime(queue.stream().map(AudioTrack::getDuration).reduce(0L, (a, b) -> a + b));
        final StringBuilder fQueue = new StringBuilder();

        final int maxPages = (int) Math.ceil((double) queue.size() / 10);
        int page = Helpers.Companion.parseNumber(context.getArgString(), 1);

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

        context.sendEmbed("Queue (" + queue.size() + " songs, " + queueDuration + ")",
                fQueue.toString().trim(),
                "Page " + page + "/" + maxPages);

    }
}
