package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.JukeBot;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Volume implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        final GuildMusicManager musicManager = JukeBot.getGuildMusicManager(e.getGuild());
        final AudioTrack currentTrack = musicManager.player.getPlayingTrack();

        if (currentTrack == null) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("No playback activity")
                    .setDescription("There's nothing playing.")
                    .build()
            ).queue();
            return;
        }

        if (query.length() == 0) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Volume")
                    .setDescription("\uD83D\uDD08 " + musicManager.player.getVolume() + "%")
                    .build()
            ).queue();
        } else {
            if (!permissions.isElevatedUser(e.getMember(), true) || !permissions.isBaller(e.getAuthor().getIdLong(), 2)) {
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Permission Error")
                        .setDescription("You need to have the DJ role and also be [a Donator!](https://www.patreon.com/Devoxin)")
                        .build()
                ).queue();
                return;
            }

            try {
                final int newVolume = Integer.parseInt(query);
                musicManager.player.setVolume(newVolume);
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Volume")
                        .setDescription("\uD83D\uDD08 " + musicManager.player.getVolume() + "%")
                        .build()
                ).queue();
            } catch (Exception err) {
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Volume")
                        .setDescription("Unable to change volume. Please ensure you specified a number.")
                        .build()
                ).queue();
            }
        }

    }
}
