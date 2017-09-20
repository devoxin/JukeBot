package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.audioutilities.SongResultHandler;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;

import java.awt.*;

public class Select implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(MessageReceivedEvent e, String query) {

        if (query.length() == 0) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Specify something")
                    .setDescription("YouTube: Search Term")
                    .build()
            ).queue();
            return;
        }

        if (!e.getMember().getVoiceState().inVoiceChannel()) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Join a voicechannel first")
                    .setDescription("You need to join a voicechannel before you can queue songs.")
                    .build()
            ).queue();
            return;
        }

        AudioManager audioManager = e.getGuild().getAudioManager();

        if (audioManager.isConnected() && !e.getMember().getVoiceState().getChannel().getId().equalsIgnoreCase(audioManager.getConnectedChannel().getId())) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Join my voicechannel")
                    .setDescription("You need to join my voicechannel before you can queue songs.")
                    .build()
            ).queue();
            return;
        }

        if (!permissions.canConnect(e.getMember().getVoiceState().getChannel())) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Invalid Channel permissions")
                    .setDescription("The target voicechannel doesn't allow me to Connect/Speak\n\nPlease reconfigure channel permissions or move to another channel.")
                    .build()
            ).queue();
            return;
        }

        final GuildMusicManager musicManager = JukeBot.getGuildMusicManager(e.getGuild());

        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            audioManager.openAudioConnection(e.getMember().getVoiceState().getChannel());
            audioManager.setSelfDeafened(true);
            musicManager.handler.setChannel(e.getTextChannel());
        }

        Bot.playerManager.loadItemOrdered(e.getGuild(), "ytsearch:" + query, new SongResultHandler(e, musicManager, true));
    }
}
