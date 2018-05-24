package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.*;

@CommandProperties(description = "Moves a track in the queue", aliases = {"m"}, category = CommandProperties.category.MEDIA)
public class Move implements Command {

    final Permissions permissions = new Permissions();

    public void execute(final Context context) {

        final AudioHandler player = context.getAudioPlayer();

        if (player.getQueue().isEmpty()) {
            context.sendEmbed("Queue is empty", "There are no tracks to move.");
            return;
        }

        final String[] args = context.getArgs();

        if (args.length < 2) {
            context.sendEmbed("Specify track index", "You need to specify the index of the track in the queue.");
            return;
        }

        final int target = Helpers.parseNumber(args[0], 0);
        final int dest = Helpers.parseNumber(args[1], 0);

        if (target < 1 || dest < 1 || target == dest || target > player.getQueue().size() || dest > player.getQueue().size()) {
            context.sendEmbed("Invalid position(s) specified!", "You need to specify a valid target track, and a valid target position.");
            return;
        }

        final AudioTrack selectedTrack = player.getQueue().get(target - 1);

        if (!context.isDJ(true)) {
            context.sendEmbed("Not a DJ", "You need the DJ role to move others' tracks. [See here on how to become a DJ](https://jukebot.xyz/faq)");
            return;
        }

        player.getQueue().remove(target - 1);
        player.getQueue().add(dest - 1, selectedTrack);

        context.sendEmbed("Track Moved", "**" + selectedTrack.getInfo().title + "** is now at position **" + dest + "** in the queue");

    }
}
