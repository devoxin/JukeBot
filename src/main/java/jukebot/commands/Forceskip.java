package jukebot.commands;

import jukebot.audio.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;

@CommandProperties(description = "Skip the track without voting", aliases = {"fs"}, category = CommandProperties.category.CONTROLS)
public class Forceskip implements Command {

    public void execute(final Context context) {

        final AudioHandler player = context.getAudioPlayer();

        if (!player.isPlaying()) {
            context.embed("Not Playing", "Nothing is currently playing.");
            return;
        }

        if (!context.isDJ(true)
                && (long) player.player.getPlayingTrack().getUserData() != context.getAuthor().getIdLong()) {

            context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.serux.pro/faq)");
            return;
        }

        player.playNext();

    }
}
