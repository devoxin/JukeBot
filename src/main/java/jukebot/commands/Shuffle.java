package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.awt.*;

public class Shuffle implements Command {

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
                    .setTitle("Pause")
                    .setDescription("You need to be in my voicechannel to toggle shuffle.")
                    .build()
            ).queue();
            return;
        }

        if (!permissions.isElevatedUser(e.getMember(), true)) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Permission Error")
                    .setDescription("You need to have the DJ role and also be [a Donator!](https://www.patreon.com/Devoxin)")
                    .build()
            ).queue();
            return;
        }

        GuildMusicManager manager = JukeBot.getGuildMusicManager(e.getGuild());
        manager.handler.shuffle = !manager.handler.shuffle;

        e.getTextChannel().sendMessage(new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .setTitle("Shuffle")
                .setDescription("Shuffle **" + (manager.handler.shuffle ? "enabled" : "disabled") + "**")
                .build()
        ).queue();

    }
}
