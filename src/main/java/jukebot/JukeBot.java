package jukebot;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.utils.Bot;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Guild;
import org.sqlite.SQLiteJDBCLoader;

import java.util.HashMap;

public class JukeBot {

    public static final long startTime = System.currentTimeMillis();

    private static final DatabaseHandler db = new DatabaseHandler();

    private static final HashMap<String, GuildMusicManager> musicManagers = new HashMap<>();

    private static Shard[] shards;

    public static void main(String[] args) throws Exception {
        System.out.println(
                ".:: JukeBot " + Bot.VERSION + " ::.\n" +
                "JDA Version: " + JDAInfo.VERSION + "\n" +
                "Lavaplayer Version: " + PlayerLibrary.VERSION + "\n" +
                "SQLite Version: " + SQLiteJDBCLoader.getVersion()
        );

        Bot.Configure();

        final int MaxShards = Integer.parseInt(db.getPropertyFromConfig("maxshards"));
        final String token = db.getPropertyFromConfig("token");

        shards = new Shard[MaxShards];

        for (int i = 0; i < MaxShards; i++) {
            try {
                shards[i] = new Shard(i, MaxShards, token);
            } catch(Exception ignored) {
                System.out.println("[" + (i + 1) + "/" + MaxShards + "] failed to login");
            }
            Thread.sleep(5000);
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
