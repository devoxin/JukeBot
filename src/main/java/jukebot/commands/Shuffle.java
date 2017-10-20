package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Shuffle implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        if (JukeBot.getGuildMusicManager(e.getGuild().getAudioManager()).player.getPlayingTrack() == null) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("No playback activity")
                    .setDescription("There's nothing playing.")
                    .build()
            ).queue();
            return;
        }

        if (!permissions.checkVoiceChannel(e.getMember())) {
                e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Shuffle")
                    .setDescription("You need to be in my voicechannel to toggle shuffle.")
                    .build()
            ).queue();
            return;
        }

        if (!permissions.isElevatedUser(e.getMember(), true)) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Permission Error")
                    .setDescription("You need to have the DJ role and also be [a Donator!](https://www.patreon.com/Devoxin)")
                    .build()
            ).queue();
            return;
        }

        GuildMusicManager manager = JukeBot.getGuildMusicManager(e.getGuild().getAudioManager());
        manager.handler.shuffle = !manager.handler.shuffle;

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .setTitle("Shuffle")
                .setDescription("Shuffle **" + (manager.handler.shuffle ? "enabled" : "disabled") + "**")
                .build()
        ).queue();

    }
}
