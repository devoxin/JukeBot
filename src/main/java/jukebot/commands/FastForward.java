package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.JukeBot;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Helpers;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class FastForward implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        final GuildMusicManager manager = JukeBot.getGuildMusicManager(e.getGuild().getAudioManager());
        final AudioTrack currentTrack = manager.player.getPlayingTrack();

        if (!manager.isPlaying()) {
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
                    .setDescription("You need to be a DJ")
                    .build()
            ).queue();
            return;
        }

        if (!currentTrack.isSeekable()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Unable to Seek")
                    .setDescription("The currently playing track doesn't support seeking.")
                    .build()
            ).queue();
            return;
        }

        int forwardTime = Helpers.ParseNumber(query, 10) * 1000;

        if (currentTrack.getPosition() + forwardTime >= currentTrack.getDuration())
            manager.handler.playNext(manager.player.getPlayingTrack());
        else {
            currentTrack.setPosition(currentTrack.getPosition() + forwardTime);
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Fast-Forward")
                    .setDescription("The current track has been fastforwarded.")
                    .build()
            ).queue();
        }

    }
}
