package jukebot.commands;

import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;
import jukebot.utils.Permissions;

@CommandProperties(description = "Skip the track without voting", aliases = {"fs"}, category = CommandProperties.category.CONTROLS)
public class Forceskip implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(final Context context) {

        final AudioHandler player = context.getAudioPlayer();

        if (!player.isPlaying()) {
            context.sendEmbed("Not Playing", "Nothing is currently playing.");
            return;
        }

        if (!context.isDJ(true)
                && (long) player.player.getPlayingTrack().getUserData() != context.getAuthor().getIdLong()) {

            context.sendEmbed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.xyz/faq)");
            return;
        }

        player.playNext();

    }
}
