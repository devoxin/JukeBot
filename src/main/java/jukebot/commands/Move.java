package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.JukeBot;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Parsers;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;

public class Move implements Command {

    final Permissions permissions = new Permissions();

    public void execute(MessageReceivedEvent e, String query) {

        final ArrayList<AudioTrack> queue = JukeBot.getGuildMusicManager(e.getGuild()).handler.getQueue();

        if (queue.size() == 0) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Queue is empty")
                    .setDescription("There is nothing to move.")
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

        final int target = Parsers.Number(query.split(" ")[0], 0);
        final int dest = Parsers.Number(query.split(" ")[1], 0);

        if (target < 1 || dest < 1 || target == dest || target > queue.size()) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Invalid position(s) specified")
                    .setDescription("You need to specify a valid target track, and a valid target position.")
                    .build()
            ).queue();
            return;
        }

        final AudioTrack selectedTrack = queue.get(target - 1);

        if (!permissions.isElevatedUser(e.getMember(), true)) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Cannot move track")
                    .setDescription("You can only move tracks that weren't queued by you if you have the DJ role.")
                    .build()
            ).queue();
            return;
        }

        queue.remove(target - 1);

        queue.add(dest - 1, selectedTrack);

        e.getTextChannel().sendMessage(new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .setTitle("Track Moved")
                .setDescription("Moved **" + selectedTrack.getInfo().title + "**")
                .build()
        ).queue();

    }
}
