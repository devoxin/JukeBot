package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.Iterator;
import java.util.LinkedList;

@CommandProperties(aliases = {"z"}, description = "Removes the last song queued by you", category = CommandProperties.category.MEDIA)
public class Undo implements Command {

    @Override
    public void execute(GuildMessageReceivedEvent e, String query) {
        AudioHandler handler = JukeBot.getPlayer(e.getGuild().getAudioManager());

        if (handler.getQueue().isEmpty()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Nothing to unqueue")
                    .setDescription("The queue is empty.")
                    .build()
            ).queue();
            return;
        }

        LinkedList<AudioTrack> queue = handler.getQueue();
        Iterator i = queue.descendingIterator();

        while (i.hasNext()) {
            AudioTrack t = (AudioTrack) i.next();
            Long requester = (long) t.getUserData();

            if (requester == e.getAuthor().getIdLong()) {
                i.remove();

                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(JukeBot.embedColour)
                        .setTitle("Track Removed")
                        .setDescription("**" + t.getInfo().title + "** removed from the queue.")
                        .build()
                ).queue();

                return;
            }
        }

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(JukeBot.embedColour)
                .setTitle("No Tracks Found")
                .setDescription("No tracks queued by you were found.")
                .build()
        ).queue();
    }
}
