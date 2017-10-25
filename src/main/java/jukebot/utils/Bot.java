package jukebot.utils;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import jukebot.ActionWaiter;
import jukebot.DatabaseHandler;
import jukebot.EventListener;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.requests.SessionReconnectQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sqlite.SQLiteJDBCLoader;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;

public class Bot {

    private static final DatabaseHandler db = new DatabaseHandler();

    public static final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    public static final ActionWaiter waiter = new ActionWaiter();

    private static final String VERSION = "6.1.0-BETA";
    public static final String defaultPrefix = db.getPropertyFromConfig("prefix");
    public static Color EmbedColour = Color.decode("#1E90FF");
    public static Long BotOwnerID = 0L;

    public static JDABuilder builder = new JDABuilder(AccountType.BOT)
            .setToken(db.getPropertyFromConfig("token"))
            .setReconnectQueue(new SessionReconnectQueue())
            .addEventListener(Bot.waiter, new EventListener())
            .setAudioSendFactory(new NativeAudioSendFactory())
            .setGame(Game.of(Bot.defaultPrefix + "help | jukebot.xyz"));

    public static final Logger LOG = LogManager.getLogger("JukeBot");

    public static void Configure() {
        Thread.currentThread().setName("JukeBot-Main");

        final String color = db.getPropertyFromConfig("color");
        if (color != null) {
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
        try {
            new BufferedReader(new FileReader("banner.txt")).lines().forEach(System.out::println);
        } catch (Exception e) {
            LOG.error("Failed to read 'banner.txt'");
        }
        System.out.println(
                "\nJukeBot v" + VERSION +
                " | JDA " + JDAInfo.VERSION +
                " | Lavaplayer " + PlayerLibrary.VERSION +
                " | SQLite " + SQLiteJDBCLoader.getVersion() +
                " | " + System.getProperty("sun.arch.data.model") + "-bit JVM\n"
        );
    }

}
