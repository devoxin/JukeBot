package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;

import java.util.Iterator;
import java.util.LinkedList;

@CommandProperties(aliases = {"z"}, description = "Removes the last song queued by you", category = CommandProperties.category.MEDIA)
public class Undo implements Command {

    @Override
    public void execute(final Context context) {

        AudioHandler handler = context.getAudioPlayer();

        if (handler.getQueue().isEmpty()) {
            context.embed("Nothing to Remove", "The queue is empty!");
            return;
        }

        LinkedList<AudioTrack> queue = handler.getQueue();
        Iterator i = queue.descendingIterator();

        while (i.hasNext()) {
            AudioTrack t = (AudioTrack) i.next();
            long requester = (long) t.getUserData();

            if (requester == context.getAuthor().getIdLong()) {
                i.remove();

                context.embed("Track Removed", "**" + t.getInfo().title + "** removed from the queue.");
                return;
            }
        }

        context.embed("No Tracks Found", "No tracks queued by you were found.");

    }
}
