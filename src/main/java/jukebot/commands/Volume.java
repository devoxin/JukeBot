package jukebot.commands;

import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;
import jukebot.utils.Helpers;

@CommandProperties(aliases = {"vol"}, description = "Adjust the player volume", category = CommandProperties.category.CONTROLS)
public class Volume implements Command {

    private final String brick = "\u25AC";
    private final int maxBricks = 10;

    public void execute(final Context context) {

        final AudioHandler player = context.getAudioPlayer();

        if (!player.isPlaying()) {
            context.embed("Not Playing", "Nothing is currently playing.");
            return;
        }

        if (context.getArgString().isEmpty()) {
            final int vol = player.player.getVolume();
            context.embed("Player Volume", calculateBricks(vol) + " `" + vol + "`");
            return;
        }

        if (!context.isDJ(false)) {
            context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.xyz/faq)");
            return;
        }

        player.player.setVolume(Math.min(Helpers.Companion.parseNumber(context.getArgString(), 100), 200));

        final int vol = player.player.getVolume();
        context.embed("Player Volume", calculateBricks(vol) + " `" + vol + "`");

    }

    private String calculateBricks(int volume) {
        final float percent = (float) volume / 200;
        final int blocks = (int) Math.floor(maxBricks * percent);

        final StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < maxBricks; i++) {
            if (i == blocks) {
                sb.append("](http://jukebot.xyz)");
            }

            sb.append(brick);
        }

        if (blocks == maxBricks) {
            sb.append("](http://jukebot.xyz)");
        }

        return sb.toString();
    }
}
