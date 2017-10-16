package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Stop implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        if (JukeBot.getGuildMusicManager(e.getGuild()).player.getPlayingTrack() == null) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("No playback activity")
                    .setDescription("There's nothing playing.")
                    .build()
            ).queue();
            return;
        }

        if (!permissions.isElevatedUser(e.getMember(), true)) {

            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Permission Error")
                    .setDescription("You need to have the DJ role!")
                    .build()
            ).queue();

            return;
        }

        final GuildMusicManager musicManager = JukeBot.getGuildMusicManager(e.getGuild());

        musicManager.handler.clearQueue();
        musicManager.handler.playNext(null);

    }
}
