package jukebot.commands;

import jukebot.audio.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;

@CommandProperties(description = "Configure track announcements", category = CommandProperties.category.MEDIA)
public class Announce implements Command {

    public void execute(final Context context) {

        final AudioHandler player = context.getAudioPlayer();
        final String[] args = context.getArgs();

        if (args[0].equalsIgnoreCase("here")) {
            player.setChannel(context.getChannel().getIdLong());
            player.setShouldAnnounce(true);
            context.embed("Track Announcements", "This channel will now be used to post track announcements");
        } else if (args[0].equalsIgnoreCase("off")) {
            player.setShouldAnnounce(false);
            context.embed("Track Announcements", "Track announcements are now disabled for this server");
        } else {
            context.embed("Track Announcements", "`here` - Uses the current channel for track announcements\n`off` - Disables track announcements");
        }
    }
}