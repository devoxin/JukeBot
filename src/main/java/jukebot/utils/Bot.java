package jukebot.utils;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import jukebot.ActionWaiter;
import jukebot.DatabaseHandler;
import net.dv8tion.jda.core.requests.SessionReconnectQueue;
import net.dv8tion.jda.core.utils.SimpleLog;

import java.awt.*;

public class Bot {

    private static final DatabaseHandler db = new DatabaseHandler();
    public static final SessionReconnectQueue reconnectQueue = new SessionReconnectQueue();
    public static ActionWaiter waiter = new ActionWaiter();

    public static final String VERSION = "6.0.20";
    public static final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    public static final String defaultPrefix = db.getPropertyFromConfig("prefix");
    public static Color EmbedColour = Color.decode("#1E90FF");

    public static void Configure() {
        final String colour = db.getPropertyFromConfig("colour");
        if (colour != null)
            try {
                EmbedColour = Color.decode(colour);
            } catch (Exception e) {
                System.out.println("Failed to decode 'colour' property in DB. Did you specify as a hex?");
            }

        YoutubeAudioSourceManager YTSM = new YoutubeAudioSourceManager();
        YTSM.setPlaylistPageCount(10);

        playerManager.setPlayerCleanupThreshold(30000);
        playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.LOW);
        playerManager.getConfiguration().setOpusEncodingQuality(9);

        playerManager.registerSourceManager(YTSM);
        AudioSourceManagers.registerRemoteSources(playerManager);

        SimpleLog.LEVEL = SimpleLog.Level.INFO;
    }

}
