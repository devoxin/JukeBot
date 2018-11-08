package jukebot.commands;

import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;
import jukebot.utils.Permissions;

@CommandProperties(description = "Plays the queue in random order", category = CommandProperties.category.CONTROLS)
public class Shuffle implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(final Context context) {

        final AudioHandler player = context.getAudioPlayer();

        if (!player.isPlaying()) {
            context.embed("Not Playing", "Nothing is currently playing.");
            return;
        }

        if (!permissions.ensureMutualVoiceChannel(context.getMember())) {
            context.embed("No Mutual VoiceChannel", "Join my VoiceChannel to use this command.");
            return;
        }

        if (!context.isDJ(true)) {
            context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.serux.pro/faq)");
            return;
        }

        context.embed("Player Shuffle", "Shuffle is now **" + (player.toggleShuffle() ? "enabled" : "disabled") + "**");

    }
}
