package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.audioutilities.SongResultHandler;
import jukebot.utils.*;
import net.dv8tion.jda.core.managers.AudioManager;

@CommandProperties(description = "Queues the track if a URL is provided, otherwise searches YouTube", aliases = {"p"}, category = CommandProperties.category.CONTROLS)
public class Play implements Command {

    final Permissions permissions = new Permissions();

    public void execute(final Context context) {

        if (context.getArgString().isEmpty()) {
            context.sendEmbed("Play", "Specify a URL or a search term");
            return;
        }

        final AudioManager manager = context.getGuild().getAudioManager();
        final AudioHandler player = context.getAudioPlayer();

        if (!permissions.checkVoiceConnection(context.getMember())) {
            context.sendEmbed("No Mutual VoiceChannel", "Join my VoiceChannel to use this command.");
            return;
        }

        if (!manager.isAttemptingToConnect() && !manager.isConnected()) {
            ConnectionError connectionStatus = permissions.canConnectTo(context.getMember().getVoiceState().getChannel());

            if (null != connectionStatus) {
                context.sendEmbed(connectionStatus.title, connectionStatus.description);
                return;
            }

            manager.openAudioConnection(context.getMember().getVoiceState().getChannel());
            player.setChannel(context.getChannel().getIdLong());
        }

        final String userQuery = context.getArgString().replaceAll("[<>]", "");

        if (userQuery.startsWith("http")) {
            if (userQuery.toLowerCase().contains("/you/likes")) {
                context.sendEmbed("SoundCloud Liked Tracks", "JukeBot doesn't implement oauth and as a result\ncannot access your liked tracks when referenced as `you`");

                if (!player.isPlaying()) {
                    manager.closeAudioConnection();
                }
                return;
            }
            if (userQuery.toLowerCase().contains("pornhub") && !context.getChannel().isNSFW()) {
                context.sendEmbed("PornHub Tracks", "PornHub tracks can only be loaded from NSFW channels!");

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
