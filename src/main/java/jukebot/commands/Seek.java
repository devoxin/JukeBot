package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.JukeBot;
import jukebot.audioutilities.MusicManager;
import jukebot.utils.Command;
import jukebot.utils.Helpers;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Seek implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        final MusicManager manager = JukeBot.getMusicManager(e.getGuild().getAudioManager());
        final AudioTrack currentTrack = manager.player.getPlayingTrack();

        if (!manager.isPlaying()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("No playback activity")
                    .setDescription("There's nothing playing.")
                    .build()
            ).queue();
            return;
        }

        if (!permissions.isElevatedUser(e.getMember(), true)) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Permission Error")
                    .setDescription("You need to be a DJ")
                    .build()
            ).queue();
            return;
        }

        if (!currentTrack.isSeekable()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Unable to Seek")
                    .setDescription("The currently playing track doesn't support seeking.")
                    .build()
            ).queue();
            return;
        }

        int forwardTime = Helpers.parseNumber(query, 10) * 1000;

        if (currentTrack.getPosition() + forwardTime >= currentTrack.getDuration())
            manager.handler.playNext();
        else {
            currentTrack.setPosition(currentTrack.getPosition() + forwardTime);
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Track Seeking")
                    .setDescription("The current track has been moved to **" + Helpers.fTime(currentTrack.getPosition()) + "**")
                    .build()
            ).queue();
        }

    }
}
