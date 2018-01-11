package jukebot;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Helpers;
import jukebot.utils.Log4JConfig;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.managers.AudioManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.sqlite.SQLiteJDBCLoader;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;

public class JukeBot {

    /* Bot-Related*/
    private static final String VERSION = "6.1.3";
    public static final long startTime = System.currentTimeMillis();
    static Logger LOG;

    static String defaultPrefix;
    public static Color embedColour;
    public static Long botOwnerId = 0L;
    public static boolean limitationsEnabled = true;
    public static int commandCount = 0;

    /* JDA-Related */
    public static AudioPlayerManager playerManager;
    private static final ConcurrentHashMap<Long, AudioHandler> players = new ConcurrentHashMap<>();
    public static final ActionWaiter waiter = new ActionWaiter();
    public static ShardManager shardManager;


    public static void main(final String[] args) throws Exception {
        Thread.currentThread().setName("JukeBot-Main");

        ConfigurationFactory.setConfigurationFactory(new Log4JConfig());
        LOG = LogManager.getLogger("JukeBot");

        playerManager = new DefaultAudioPlayerManager();
        defaultPrefix = Database.getPropertyFromConfig("prefix");

        String colour = Database.getPropertyFromConfig("color");
        if (colour.equalsIgnoreCase("")) {
            LOG.error("Missing property 'color' in the database.");
            return;
        }

        embedColour = Color.decode(colour);

        playerManager.setPlayerCleanupThreshold(30000);
        playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.LOW);
        playerManager.getConfiguration().setOpusEncodingQuality(9);
        YoutubeAudioSourceManager yt = new YoutubeAudioSourceManager();
        yt.setPlaylistPageCount(Integer.MAX_VALUE);
        playerManager.registerSourceManager(yt);
        AudioSourceManagers.registerRemoteSources(playerManager);

        printBanner();

        shardManager = new DefaultShardManagerBuilder()
                .setToken(Database.getPropertyFromConfig("token"))
                .setShardsTotal(-1)
                .addEventListeners(new EventListener(), waiter)
                .setAudioSendFactory(new NativeAudioSendFactory())
                .setGame(Game.of(Game.GameType.LISTENING, defaultPrefix + "help | jukebot.xyz"))
                .build();
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

    public static ConcurrentHashMap<Long, AudioHandler> getPlayers() {
        return players;
    }

    public static AudioHandler getPlayer(final AudioManager manager) {

        AudioHandler handler = players.computeIfAbsent(manager.getGuild().getIdLong(), v -> new AudioHandler(playerManager.createPlayer()));

        if (manager.getSendingHandler() == null)
            manager.setSendingHandler(handler);

        return handler;

    }

}
