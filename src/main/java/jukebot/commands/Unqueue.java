package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;
import jukebot.utils.Helpers;

@CommandProperties(description = "Remove a track from the queue", aliases = {"uq", "remove", "r"}, category = CommandProperties.category.MEDIA)
public class Unqueue implements Command {

    public void execute(final Context context) {

        final AudioHandler player = context.getAudioPlayer();

        if (player.getQueue().isEmpty()) {
            context.embed("Nothing to Unqueue", "The queue is empty!");
            return;
        }

        if (context.getArgString().isEmpty()) {
            context.embed("Specify track index", "You need to specify the index of the track to unqueue.");
            return;
        }

        final int selected = Helpers.Companion.parseNumber(context.getArgString(), 0);

        if (selected < 1 || selected > player.getQueue().size()) {
            context.embed("Invalid position specified!", "You need to specify a valid target track.");
            return;
        }

        final AudioTrack selectedTrack = player.getQueue().get(selected - 1);

        if ((long) selectedTrack.getUserData() != context.getAuthor().getIdLong() && !context.isDJ(false)) {
            context.embed("Not a DJ", "You need the DJ role to unqueue others' tracks. [See here on how to become a DJ](https://jukebot.serux.pro/faq)");
            return;
        }

        player.getQueue().remove(selected - 1);

        context.embed("Track Unqueued", "Removed **" + selectedTrack.getInfo().title + "** from the queue.");

    }
}
