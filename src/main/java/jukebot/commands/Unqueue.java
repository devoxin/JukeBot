package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.Helpers;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Unqueue implements Command {

    final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        final AudioHandler handler = JukeBot.getMusicManager(e.getGuild().getAudioManager()).handler;

        if (handler.getQueue().isEmpty()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Queue is empty")
                    .setDescription("There is nothing to unqueue.")
                    .build()
            ).queue();
            return;
        }

        if (query.length() == 0) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Specify song position")
                    .setDescription("You need to specify the position of the song in the queue.")
                    .build()
            ).queue();
            return;
        }

        final int selected = Helpers.ParseNumber(query, 0);

        if (selected < 1 || selected > handler.getQueue().size()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Invalid position specified")
                    .setDescription("You need to specify a valid number.")
                    .build()
            ).queue();
            return;
        }

        final AudioTrack selectedTrack = handler.getQueue().get(selected - 1);

        if ((long) selectedTrack.getUserData() != e.getAuthor().getIdLong() && !permissions.isElevatedUser(e.getMember(), false)) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Cannot unqueue track")
                    .setDescription("You need to have the DJ role to unqueue other users' tracks.")
                    .build()
            ).queue();
            return;
        }

        handler.getQueue().remove(selected - 1);

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(JukeBot.EmbedColour)
                .setTitle("Track Unqueued")
                .setDescription("Removed **" + selectedTrack.getInfo().title + "** from the queue.")
                .build()
        ).queue();

    }
}
