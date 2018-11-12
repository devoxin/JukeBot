package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audio.AudioHandler;
import jukebot.audio.SongResultHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;

@CommandProperties(description = "Search SoundCloud and queue the top result", aliases = {"sc"}, category = CommandProperties.category.CONTROLS)
public class ScSearch implements Command {

    public void execute(final Context context) {

        if (context.getArgString().isEmpty()) {
            context.embed("SoundCloud Search", "Specify what to search for.");
            return;
        }

        final AudioHandler player = context.getAudioPlayer();
        final boolean voiceConnected = context.ensureVoice();

        if (!voiceConnected) {
            return;
        }

        if (!player.isPlaying()) {
            player.setChannel(context.getChannel().getIdLong());
        }

        JukeBot.playerManager.loadItem("scsearch:" + context.getArgString(), new SongResultHandler(context, player, false));

    }
}
