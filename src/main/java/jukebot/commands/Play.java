package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.audioutilities.SongResultHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;
import net.dv8tion.jda.core.managers.AudioManager;

@CommandProperties(description = "Finds and plays the provided song query/URL", aliases = {"p"}, category = CommandProperties.category.CONTROLS)
public class Play implements Command {

    public void execute(final Context context) {

        if (context.getArgString().isEmpty()) {
            context.embed("Play", "Specify a URL or a search term");
            return;
        }

        final AudioManager manager = context.getGuild().getAudioManager();
        final AudioHandler player = context.getAudioPlayer();

        final boolean voiceConnected = context.ensureVoice();

        if (!voiceConnected) {
            return;
        }

        if (!player.isPlaying()) {
            player.setChannel(context.getChannel().getIdLong());
        }

        final String userQuery = context.getArgString().replaceAll("[<>]", "");

        if (userQuery.startsWith("http")) {
            if (userQuery.toLowerCase().contains("/you/likes")) {
                context.embed("SoundCloud Liked Tracks", "JukeBot doesn't implement oauth and as a result\ncannot access your liked tracks when referenced as `you`");

                if (!player.isPlaying()) {
                    manager.closeAudioConnection();
                }
                return;
            }
            if (userQuery.toLowerCase().contains("pornhub") && !context.getChannel().isNSFW()) {
                context.embed("PornHub Tracks", "PornHub tracks can only be loaded from NSFW channels!");

                if (!player.isPlaying()) {
                    manager.closeAudioConnection();
                }

                return;
            }
            JukeBot.playerManager.loadItem(userQuery, new SongResultHandler(context, player, false));
        } else {
            JukeBot.playerManager.loadItem("ytsearch:" + userQuery, new SongResultHandler(context, player, false));
        }

    }
}
