package jukebot.commands;

import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.JukeBot;
import jukebot.utils.Permissions;
import jukebot.audioutilities.GuildMusicManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.awt.Color;

public class Forceskip implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(MessageReceivedEvent e, String query) {

        final GuildMusicManager musicManager = JukeBot.getGuildMusicManager(e.getGuild());

        if (!e.getGuild().getAudioManager().isConnected() || musicManager.player.getPlayingTrack() == null) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("No playback activity")
                    .setDescription("There's nothing playing.")
                    .build()
            ).queue();
            return;
        }

        if (!permissions.isElevatedUser(e.getMember(), true) && !e.getAuthor().getId().equalsIgnoreCase(musicManager.player.getPlayingTrack().getUserData().toString())) {

            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Permission Error")
                    .setDescription("You need to have the DJ role!")
                    .build()
            ).queue();

            return;
        }

        musicManager.handler.playNext(null);

    }
}
