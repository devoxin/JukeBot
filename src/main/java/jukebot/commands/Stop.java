package jukebot.commands;

import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.JukeBot;
import jukebot.utils.Permissions;
import jukebot.audioutilities.GuildMusicManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.awt.Color;

public class Stop implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(MessageReceivedEvent e, String query) {

        if (!e.getGuild().getAudioManager().isConnected()) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("No playback activity")
                    .setDescription("There's nothing playing.")
                    .build()
            ).queue();
            return;
        }

        if (!permissions.isElevatedUser(e.getMember(), true)) {

            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Permission Error")
                    .setDescription("You don't have permission to use this command.\n\n(DJ role required)")
                    .build()
            ).queue();

            return;
        }

        final GuildMusicManager musicManager = JukeBot.getGuildMusicManager(e.getGuild());

        musicManager.handler.clearQueue();
        musicManager.handler.playNext(null);

    }
}
