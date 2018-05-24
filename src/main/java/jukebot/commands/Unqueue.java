package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.*;

@CommandProperties(description = "Remove a track from the queue", aliases = {"uq", "remove", "r"}, category = CommandProperties.category.MEDIA)
public class Unqueue implements Command {

    final Permissions permissions = new Permissions();

    public void execute(final Context context) {

        final AudioHandler player = context.getAudioPlayer();

        if (player.getQueue().isEmpty()) {
            context.sendEmbed("Nothing to Unqueue", "The queue is empty!");
            return;
        }

        if (context.getArgString().isEmpty()) {
            context.sendEmbed("Specify track index", "You need to specify the index of the track to unqueue.");
            return;
        }

        final int selected = Helpers.parseNumber(context.getArgString(), 0);

        if (selected < 1 || selected > player.getQueue().size()) {
            context.sendEmbed("Invalid position specified!", "You need to specify a valid target track.");
            return;
        }

        final AudioTrack selectedTrack = player.getQueue().get(selected - 1);

        if ((long) selectedTrack.getUserData() != context.getAuthor().getIdLong() && !context.isDJ(false)) {
            context.sendEmbed("Not a DJ", "You need the DJ role to unqueue others' tracks. [See here on how to become a DJ](https://jukebot.xyz/faq)");
            return;
        }

        player.getQueue().remove(selected - 1);

        context.sendEmbed("Track Unqueued", "Removed **" + selectedTrack.getInfo().title + "** from the queue.");

    }
}
