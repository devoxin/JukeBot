package jukebot.utils;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import jukebot.ActionWaiter;
import jukebot.Database;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.requests.SessionReconnectQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sqlite.SQLiteJDBCLoader;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Bot {

    public static final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    public static final ActionWaiter waiter = new ActionWaiter();
    public static final SessionReconnectQueue sesh = new SessionReconnectQueue();

    private static final String VERSION = "6.1.0-BETA";
    public static final String defaultPrefix = Database.getPropertyFromConfig("prefix");
    public static Color EmbedColour = Color.decode("#1E90FF");
    public static Long BotOwnerID = 0L;

    public static final Logger LOG = LogManager.getLogger("JukeBot");

    public static void Configure() {
        Thread.currentThread().setName("JukeBot-Main");

        final String color = Database.getPropertyFromConfig("color");
        if (!"".equals(color)) {
            try {
                EmbedColour = Color.decode(color);
            } catch (Exception e) {
                LOG.error("Failed to decode 'colour' property in DB. Did you specify a hex? (e.g. 0xFFFFFF or #FFFFFF)");
            }
        }

        playerManager.setPlayerCleanupThreshold(30000);
        playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.LOW);
        playerManager.getConfiguration().setOpusEncodingQuality(9);
        AudioSourceManagers.registerRemoteSources(playerManager);

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

}
