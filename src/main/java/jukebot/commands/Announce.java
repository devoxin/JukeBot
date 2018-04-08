package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(description = "Configure track announcements", category = CommandProperties.category.MEDIA)
public class Announce implements Command {

    public void execute(GuildMessageReceivedEvent e, String query) {

        final AudioHandler player = JukeBot.getPlayer(e.getGuild().getAudioManager());
        final String[] args = query.split("\\s+");

        if (args[0].equalsIgnoreCase("here")) {
            player.setChannel(e.getChannel().getIdLong());
            player.setShouldAnnounce(true);

            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Track Announcements")
                    .setDescription("This channel will now be used to post track announcements")
                    .build()
            ).queue();
        } else if (args[0].equalsIgnoreCase("off")) {
            player.setShouldAnnounce(false);

            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Track Announcements")
                    .setDescription("Track announcements are now disabled for this server")
                    .build()
            ).queue();
        } else {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Track Announcements")
                    .setDescription("`here` - Uses the current channel for track announcements\n`off` - Disables track announcements")
                    .build()
            ).queue();
        }
    }
}