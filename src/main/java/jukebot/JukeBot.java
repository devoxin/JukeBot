package jukebot;

import com.patreon.PatreonAPI;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import jukebot.audioutilities.AudioHandler;
import jukebot.audioutilities.PornHubAudioSourceManager;
import jukebot.utils.Helpers;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteJDBCLoader;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;

public class JukeBot {

    /* Bot-Related*/
    public static final String VERSION = "6.1.9";
    public static final Long startTime = System.currentTimeMillis();
    public static Logger LOG = LoggerFactory.getLogger("JukeBot");
    public static boolean hasFinishedLoading = false;

    static String defaultPrefix = Database.getPropertyFromConfig("prefix", "$");
    public static Color embedColour = Color.decode(Database.getPropertyFromConfig("color", "0x1E90FF"));
    public static Long botOwnerId = 0L;
    public static boolean isSelfHosted = false;

    /* Operation-Related */
    public static PatreonAPI patreonApi = new PatreonAPI(Database.getPropertyFromConfig("patreon", null));
    private static final ConcurrentHashMap<Long, AudioHandler> players = new ConcurrentHashMap<>();
    public static final ActionWaiter waiter = new ActionWaiter();
    public static AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    public static ShardManager shardManager;


    public static void main(final String[] args) throws Exception {
        Thread.currentThread().setName("JukeBot-Main");

        playerManager.setPlayerCleanupThreshold(30000);
        playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.LOW);
        playerManager.getConfiguration().setOpusEncodingQuality(9);
        playerManager.registerSourceManager(new PornHubAudioSourceManager());
        YoutubeAudioSourceManager yt = new YoutubeAudioSourceManager();
        yt.setPlaylistPageCount(Integer.MAX_VALUE);
        playerManager.registerSourceManager(yt);
        AudioSourceManagers.registerRemoteSources(playerManager);

        printBanner();

        DefaultShardManagerBuilder shardManagerBuilder = new DefaultShardManagerBuilder()
                .setToken(Database.getPropertyFromConfig("token", null))
                .setShardsTotal(-1)
                .addEventListeners(new CommandHandler(), waiter)
                .setGame(Game.of(Game.GameType.LISTENING, defaultPrefix + "help | jukebot.xyz"));

        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch");

        if ((os.contains("windows") || os.contains("linux")) && !arch.equalsIgnoreCase("arm") && !arch.equalsIgnoreCase("arm-linux")) {
            LOG.info("System supports NAS, enabling...");
            shardManagerBuilder.setAudioSendFactory(new NativeAudioSendFactory());
        }

        shardManager = shardManagerBuilder.build();
    }


    private static void printBanner() {
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        String banner = Helpers.readFile("banner.txt");

        if (banner != null)
            LOG.info("\n" + banner);

        LOG.info(
                "\nJukeBot v" + VERSION +
                        " | JDA " + JDAInfo.VERSION +
                        " | Lavaplayer " + PlayerLibrary.VERSION +
                        " | SQLite " + SQLiteJDBCLoader.getVersion() +
                        " | " + System.getProperty("sun.arch.data.model") + "-bit JVM" +
                        " | " + os + " " + arch + "\n"
        );
    }

    public static ConcurrentHashMap<Long, AudioHandler> getPlayers() {
        return players;
    }

    public static boolean hasPlayer(final long guildId) {
        return players.containsKey(guildId);
    }

    public static AudioHandler getPlayer(final AudioManager manager) {

        AudioHandler handler = players.computeIfAbsent(manager.getGuild().getIdLong(),
                v -> new AudioHandler(manager.getGuild().getIdLong(), playerManager.createPlayer()));

        if (manager.getSendingHandler() == null)
            manager.setSendingHandler(handler);

        return handler;

    }

    public static void removePlayer(final long guildId) {
        if (JukeBot.hasPlayer(guildId)) {
            players.remove(guildId).cleanup();
        }
    }

    public static void recreatePatreonApi(String key) {
        patreonApi = new PatreonAPI(key);
    }

}
