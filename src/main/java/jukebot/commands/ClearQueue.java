package jukebot.commands;

import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;

@CommandProperties(description = "Removes all of the tracks from the queue", aliases = {"cq", "c", "clear", "empty"}, category = CommandProperties.category.MEDIA)
public class ClearQueue implements Command {

    public void execute(final Context context) {

        final AudioHandler player = context.getAudioPlayer();

        if (player.getQueue().isEmpty()) {
            context.embed("Nothing to Clear", "The queue is already empty!");
            return;
        }

        if (!context.isDJ(true)) {
            context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.xyz/faq)");
            return;
        }

        player.getQueue().clear();

        context.embed("Queue Cleared", "The queue is now empty.");

    }

}
