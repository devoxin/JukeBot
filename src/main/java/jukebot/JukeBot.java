package jukebot;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import jukebot.audioutilities.MusicManager;
import jukebot.utils.Bot;
import jukebot.utils.Log4JConfig;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.managers.AudioManager;
import net.dv8tion.jda.core.requests.SessionReconnectQueue;
import org.apache.logging.log4j.core.config.ConfigurationFactory;

import java.util.HashMap;

public class JukeBot {

    public static final long startTime = System.currentTimeMillis();

    private static final DatabaseHandler db = new DatabaseHandler();
    private static final HashMap<Long, MusicManager> musicManagers = new HashMap<>();

    private static final Shard[] shards = new Shard[Integer.parseInt(db.getPropertyFromConfig("maxshards"))];

    public static void main(final String[] args) throws Exception {
        ConfigurationFactory.setConfigurationFactory(new Log4JConfig());
        Bot.Configure();

        for (int i = 0; i < shards.length; i++) {
            shards[i] = new Shard(i, shards.length);
            Thread.sleep(5500);
        }

    }

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
