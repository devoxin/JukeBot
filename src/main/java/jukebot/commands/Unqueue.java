package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.JukeBot;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Parsers;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.ArrayList;

public class Unqueue implements Command {

    final Permissions permissions = new Permissions();

    public void execute(MessageReceivedEvent e, String query) {

        final ArrayList<AudioTrack> queue = JukeBot.getGuildMusicManager(e.getGuild()).handler.getQueue();

        if (queue.size() == 0) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Queue is empty")
                    .setDescription("There is nothing to unqueue.")
                    .build()
            ).queue();
            return;
        }

        if (query.length() == 0) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Specify song position")
                    .setDescription("You need to specify the position of the song in the queue.")
                    .build()
            ).queue();
            return;
        }

        final int selected = Parsers.Number(query, 0);

        if (selected < 1 || selected > queue.size()) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Invalid position specified")
                    .setDescription("You need to specify a valid number.")
                    .build()
            ).queue();
            return;
        }

        final AudioTrack selectedTrack = queue.get(selected - 1);

        if (!selectedTrack.getUserData().equals(e.getAuthor().getId()) && !permissions.isElevatedUser(e.getMember(), false)) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Cannot unqueue track")
                    .setDescription("You can only unqueue tracks that weren't queued by you if you have the DJ role.")
                    .build()
            ).queue();
            return;
        }

        queue.remove(selected - 1);

        e.getTextChannel().sendMessage(new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .setTitle("Track Unqueued")
                .setDescription("Removed **" + selectedTrack.getInfo().title + "** from the queue.")
                .build()
        ).queue();

    }
}
