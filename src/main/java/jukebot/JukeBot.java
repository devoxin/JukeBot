package jukebot;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import jukebot.audioutilities.MusicManager;
import jukebot.utils.Log4JConfig;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.managers.AudioManager;
import net.dv8tion.jda.core.requests.SessionReconnectQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.sqlite.SQLiteJDBCLoader;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class JukeBot {

    /* Bot-Related*/
    public static final long startTime = System.currentTimeMillis();
    private static final String VERSION = "6.1.0-BETA";
    static final String defaultPrefix = Database.getPropertyFromConfig("prefix");
    public static Color EmbedColour = Color.decode("#1E90FF");
    public static Long BotOwnerID = 0L;

    /* JDA-Related */
    public static final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private static final HashMap<Long, MusicManager> musicManagers = new HashMap<>();
    public static final ActionWaiter waiter = new ActionWaiter();
    static final SessionReconnectQueue sesh = new SessionReconnectQueue();
    private static Shard[] shards = new Shard[Integer.parseInt(Database.getPropertyFromConfig("maxshards"))];

    /* Misc */
    static Logger LOG = LogManager.getLogger("JukeBot");


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
        try (
                final FileReader file = new FileReader("banner.txt");
                final BufferedReader reader = new BufferedReader(file)
        )
        {
            reader.lines().forEach(System.out::println);
        } catch (IOException unused) {}

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
    public static HashMap<Long, MusicManager> getMusicManagers() {
        return musicManagers;
    }
    public static MusicManager getMusicManager(final AudioManager manager) {

        MusicManager musicManager = musicManagers.computeIfAbsent(manager.getGuild().getIdLong(), v -> new MusicManager());

        if (manager.getSendingHandler() == null)
            manager.setSendingHandler(musicManager.handler);

        return musicManager;

    }

}
