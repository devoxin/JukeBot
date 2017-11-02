package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.MusicManager;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(aliases = {"ph"})
public class PostHere implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        final MusicManager musicManager = JukeBot.getMusicManager(e.getGuild().getAudioManager());

        if (!musicManager.isPlaying()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("No playback activity")
                    .setDescription("There's nothing playing.")
                    .build()
            ).queue();
            return;
        }

        if (!permissions.isElevatedUser(e.getMember(), false)) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Permission Error")
                    .setDescription("You need to have the DJ role.")
                    .build()
            ).queue();
            return;
        }

        musicManager.handler.setChannel(e.getChannel());

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(JukeBot.EmbedColour)
                .setTitle("Output Channel Set")
                .setDescription("Now Playing messages will now be posted here.")
                .build()
        ).queue();

    }
}