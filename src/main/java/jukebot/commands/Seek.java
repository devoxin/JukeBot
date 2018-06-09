package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.*;

@CommandProperties(description = "Move to the specified position in the track", category = CommandProperties.category.CONTROLS)
public class Seek implements Command {

    public void execute(final Context context) {

        final AudioHandler player = context.getAudioPlayer();
        final AudioTrack currentTrack = player.player.getPlayingTrack();

        if (!player.isPlaying()) {
            context.sendEmbed("Not Playing", "Nothing is currently playing.");
            return;
        }

        if (!context.isDJ(true)) {
            context.sendEmbed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.xyz/faq)");
            return;
        }

        if (!currentTrack.isSeekable()) {
            context.sendEmbed("Seek Unavailable", "The current track doesn't support seeking.");
            return;
        }

        int forwardTime = Helpers.parseNumber(context.getArgString(), 10) * 1000;

        if (currentTrack.getPosition() + forwardTime >= currentTrack.getDuration()) {
            player.playNext();
            return;
        }

        currentTrack.setPosition(currentTrack.getPosition() + forwardTime);

        context.sendEmbed("Track Seeking", "The current track has been moved to **" + Helpers.fTime(currentTrack.getPosition()) + "**");

    }
}
