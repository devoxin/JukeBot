package jukebot;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Helpers;
import jukebot.utils.Log4JConfig;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.managers.AudioManager;
import net.dv8tion.jda.core.requests.SessionReconnectQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.sqlite.SQLiteJDBCLoader;

import java.awt.*;
import java.util.HashMap;

public class JukeBot {

    /* Bot-Related*/
    public static final long startTime = System.currentTimeMillis();
    private static final String VERSION = "6.1.1";
    static final String defaultPrefix = Database.getPropertyFromConfig("prefix");
    public static Color EmbedColour = Color.decode("#1E90FF");
    public static Long BotOwnerID = 0L;
    public static boolean limitationsEnabled = true;

    /* JDA-Related */
    public static final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private static final HashMap<Long, AudioHandler> musicManagers = new HashMap<>();
    public static final ActionWaiter waiter = new ActionWaiter();
    static final SessionReconnectQueue sesh = new SessionReconnectQueue();
    private static Shard[] shards = new Shard[Integer.parseInt(Database.getPropertyFromConfig("maxshards"))];

    /* Misc */
    static Logger LOG = LogManager.getLogger("JukeBot");
    static int commandCount = 0;


    /* Functions */
    public static void main(final String[] args) throws Exception {
        ConfigurationFactory.setConfigurationFactory(new Log4JConfig());
        setupJukeBot();

        for (int i = 0; i < shards.length; i++) {
            shards[i] = new Shard(i, shards.length);
            Thread.sleep(5500);
        }
    }

    /* Self-Configuration Functions */
    /**
     * Delegating tasks to the below function allows the above code to look cleaner lol
     */
    private static void setupJukeBot() {
        Thread.currentThread().setName("JukeBot-Main");

        playerManager.setPlayerCleanupThreshold(30000);
        playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.LOW);
        playerManager.getConfiguration().setOpusEncodingQuality(9);
        YoutubeAudioSourceManager yt = new YoutubeAudioSourceManager();
        yt.setPlaylistPageCount(Integer.MAX_VALUE);
        playerManager.registerSourceManager(yt);
        AudioSourceManagers.registerRemoteSources(playerManager);

        try {
            final String color = Database.getPropertyFromConfig("color");
            if (!"".equals(color))
                EmbedColour = Color.decode(color);
        } catch (Exception e) {
            LOG.warn("Failed to decode color. Ensure you specified a hex (0xFFF, #FFF)");
        }

        printBanner();
    }

    private static void printBanner() {
        System.out.println(Helpers.readFile("banner.txt"));
        System.out.println(
                "\nJukeBot v" + VERSION +
                " | JDA " + JDAInfo.VERSION +
                " | Lavaplayer " + PlayerLibrary.VERSION +
                " | SQLite " + SQLiteJDBCLoader.getVersion() +
                " | " + System.getProperty("sun.arch.data.model") + "-bit JVM\n"
        );
    }

    /* Retrieval Functions */

    public static Shard[] getShards() {
        return shards;
    }
    public static HashMap<Long, AudioHandler> getMusicManagers() {
        return musicManagers;
    }
    public static AudioHandler getMusicManager(final AudioManager manager) {

        AudioHandler handler = musicManagers.computeIfAbsent(manager.getGuild().getIdLong(), v -> new AudioHandler(playerManager.createPlayer()));

        if (manager.getSendingHandler() == null)
            manager.setSendingHandler(handler);

        return handler;

    }

}
