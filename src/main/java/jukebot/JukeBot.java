package jukebot;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.utils.Bot;
import jukebot.utils.Log4JConfig;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Guild;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.sqlite.SQLiteJDBCLoader;

import java.util.HashMap;

import static jukebot.utils.Bot.LOG;

public class JukeBot {

    public static final long startTime = System.currentTimeMillis();

    private static final DatabaseHandler db = new DatabaseHandler();

    private static final HashMap<String, GuildMusicManager> musicManagers = new HashMap<>();

    private static Shard[] shards;

    public static void main(String[] args) throws Exception {
        /* Use default log config--only log INFO+ messages */
        ConfigurationFactory.setConfigurationFactory(new Log4JConfig());
        LOG.info(".:: JukeBot " + Bot.VERSION + " ::.\n" +
                ":: JDA: " + JDAInfo.VERSION + "\n" +
                ":: Lavaplayer: " + PlayerLibrary.VERSION + "\n" +
                ":: SQLite: " + SQLiteJDBCLoader.getVersion());

        Bot.Configure();

        final int MaxShards = Integer.parseInt(db.getPropertyFromConfig("maxshards"));

        shards = new Shard[MaxShards];

        for (int i = 0; i < MaxShards; i++) {
            try {
                shards[i] = new Shard(i, MaxShards);
            } catch(Exception ignored) {
                LOG.error("[" + (i + 1) + "/" + MaxShards + "] failed to login");
            }
            Thread.sleep(5500);
        }

    }

    public static Shard[] getShards() {
        return shards;
    }

    public static HashMap<String, GuildMusicManager> getMusicManagers() {
        return musicManagers;
    }

    public static GuildMusicManager getGuildMusicManager(final Guild guild) {

        GuildMusicManager musicManager = musicManagers.computeIfAbsent(guild.getId(), k -> new GuildMusicManager());

        if (guild.getAudioManager().getSendingHandler() == null)
            guild.getAudioManager().setSendingHandler(musicManager.handler);

        return musicManager;

    }

}
