package jukebot.commands;

import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.JukeBot;
import jukebot.utils.Permissions;
import jukebot.audioutilities.GuildMusicManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.awt.Color;

public class Resume implements Command {

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

        if (!e.getMember().getVoiceState().inVoiceChannel() ||
                e.getGuild().getAudioManager().isConnected() && !e.getMember().getVoiceState().getChannel().getId().equalsIgnoreCase(e.getGuild().getAudioManager().getConnectedChannel().getId())) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Resume")
                    .setDescription("You need to be in my voicechannel to resume.")
                    .build()
            ).queue();
            return;
        }

        if (!permissions.isElevatedUser(e.getMember(), true)) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Permission Error")
                    .setDescription("You need to have the DJ role!")
                    .build()
            ).queue();
            return;
        }

        final GuildMusicManager musicManager = JukeBot.getGuildMusicManager(e.getGuild());

        musicManager.player.setPaused(false);

    }
}
