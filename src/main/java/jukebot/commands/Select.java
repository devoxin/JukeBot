package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.audioutilities.SongResultHandler;
import jukebot.utils.*;
import net.dv8tion.jda.core.managers.AudioManager;

@CommandProperties(description = "Search YouTube and select from up to 5 tracks", aliases = {"sel", "s"}, category = CommandProperties.category.CONTROLS)
public class Select implements Command {

    final Permissions permissions = new Permissions();

    public void execute(final Context context) {

        if (context.getArgString().isEmpty()) {
            context.sendEmbed("YouTube Search", "Specify what to search for.");
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

        JukeBot.playerManager.loadItem("ytsearch:" + context.getArgString(), new SongResultHandler(context, player, true));
    }
}
