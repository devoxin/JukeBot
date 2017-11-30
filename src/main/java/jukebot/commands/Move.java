package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Helpers;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(description = "Moves a track in the queue", aliases = {"m"}, category = CommandProperties.category.MEDIA)
public class Move implements Command {

    final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        final AudioHandler handler = JukeBot.getMusicManager(e.getGuild().getAudioManager()).handler;

        if (handler.getQueue().isEmpty()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Queue is empty")
                    .setDescription("There is nothing to move.")
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

        final int target = Helpers.parseNumber(query.split(" ")[0], 0);
        final int dest = Helpers.parseNumber(query.split(" ")[1], 0);

        if (target < 1 || dest < 1 || target == dest || target > handler.getQueue().size()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Invalid position(s) specified")
                    .setDescription("You need to specify a valid target track, and a valid target position.")
                    .build()
            ).queue();
            return;
        }

        final AudioTrack selectedTrack = handler.getQueue().get(target - 1);

        if (!permissions.isElevatedUser(e.getMember(), true)) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Cannot move track")
                    .setDescription("You need the DJ role to move other users' tracks.")
                    .build()
            ).queue();
            return;
        }

        handler.getQueue().remove(target - 1);
        handler.getQueue().add(dest - 1, selectedTrack);

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(JukeBot.EmbedColour)
                .setTitle("Track Moved")
                .setDescription("Moved **" + selectedTrack.getInfo().title + "**")
                .build()
        ).queue();

    }
}
