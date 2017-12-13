package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Helpers;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(description = "Move to the specified position in the track", category = CommandProperties.category.CONTROLS)
public class Seek implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        final AudioHandler player = JukeBot.getPlayer(e.getGuild().getAudioManager());
        final AudioTrack currentTrack = player.player.getPlayingTrack();

        if (!player.isPlaying()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("No playback activity")
                    .setDescription("There's nothing playing.")
                    .build()
            ).queue();
            return;
        }

        if (!permissions.isElevatedUser(e.getMember(), true)) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Permission Error")
                    .setDescription("You need to be a DJ")
                    .build()
            ).queue();
            return;
        }

        if (!currentTrack.isSeekable()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Unable to Seek")
                    .setDescription("The currently playing track doesn't support seeking.")
                    .build()
            ).queue();
            return;
        }

        int forwardTime = Helpers.parseNumber(query, 10) * 1000;

        if (currentTrack.getPosition() + forwardTime >= currentTrack.getDuration()) {
            player.playNext();
            return;
        }

        currentTrack.setPosition(currentTrack.getPosition() + forwardTime);
        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(JukeBot.embedColour)
                .setTitle("Track Seeking")
                .setDescription("The current track has been moved to **" + Helpers.fTime(currentTrack.getPosition()) + "**")
                .build()
        ).queue();

    }
}
