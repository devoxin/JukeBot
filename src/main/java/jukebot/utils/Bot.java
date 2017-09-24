package jukebot.utils;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import jukebot.ActionWaiter;
import jukebot.DatabaseHandler;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.requests.SessionReconnectQueue;

import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Bot {

    private static final DatabaseHandler db = new DatabaseHandler();
    private static final SessionReconnectQueue reconnectQueue = new SessionReconnectQueue();
    public static ActionWaiter waiter = new ActionWaiter();

    public static final String VERSION = "6.0.21";
    public static final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    public static final String defaultPrefix = db.getPropertyFromConfig("prefix");
    public static Color EmbedColour = Color.decode("#1E90FF");

    private static final DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public static JDABuilder builder = new JDABuilder(AccountType.BOT)
            .setToken(db.getPropertyFromConfig("token"))
            .setReconnectQueue(Bot.reconnectQueue)
            .addEventListener(waiter);

    public static void Configure() {
        final String colour = db.getPropertyFromConfig("colour");
        if (colour != null)
            try {
                EmbedColour = Color.decode(colour);
            } catch (Exception e) {
                Bot.Log("Failed to decode 'colour' property in DB. Did you specify as a hex?", LOGTYPE.ERROR);
            }

        YoutubeAudioSourceManager YTSM = new YoutubeAudioSourceManager();
        YTSM.setPlaylistPageCount(10);

        playerManager.setPlayerCleanupThreshold(30000);
        playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.LOW);
        playerManager.getConfiguration().setOpusEncodingQuality(9);

        playerManager.registerSourceManager(YTSM);
        AudioSourceManagers.registerRemoteSources(playerManager);

    }

    public static void Log(String message, LOGTYPE type) {
        Date date = new Date();
        switch (type) {
            case INFORMATION:
                System.out.println("[" + timeFormat.format(date) + "] [INFO] " + message);
                break;
            case WARNING:
                System.out.println("[" + timeFormat.format(date) + "] [WARN] " + message);
                break;
            case ERROR:
                System.out.println("[" + timeFormat.format(date) + "] [ERR ] " + message);
                break;
        }
    }

    public enum LOGTYPE {
        ERROR,
        WARNING,
        INFORMATION
    }

    public enum REPEATMODE {
        SINGLE,
        ALL,
        NONE
    }

}
