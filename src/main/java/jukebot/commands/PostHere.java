package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(description = "Uses the current channel for track announcements", aliases = {"ph"}, category = CommandProperties.category.MEDIA)
public class PostHere implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        final AudioHandler player = JukeBot.getPlayer(e.getGuild().getAudioManager());

        if (!player.isPlaying()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("No playback activity")
                    .setDescription("There's nothing playing.")
                    .build()
            ).queue();
            return;
        }

        if (!permissions.isElevatedUser(e.getMember(), false)) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Permission Error")
                    .setDescription("You need to have the DJ role.")
                    .build()
            ).queue();
            return;
        }

        player.setChannel(e.getChannel().getIdLong());

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(JukeBot.embedColour)
                .setTitle("Output Channel Set")
                .setDescription("Now Playing messages will now be posted here.")
                .build()
        ).queue();

    }
}