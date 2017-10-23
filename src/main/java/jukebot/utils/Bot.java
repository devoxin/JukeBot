package jukebot.utils;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import jukebot.ActionWaiter;
import jukebot.DatabaseHandler;
import jukebot.EventListener;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.requests.SessionReconnectQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;

public class Bot {

    private static final DatabaseHandler db = new DatabaseHandler();

    public static final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    public static final ActionWaiter waiter = new ActionWaiter();

    public static final String VERSION = "6.0.24-DEV"; // tfw it goes from beta to dev bc it's so borked
    public static final String defaultPrefix = db.getPropertyFromConfig("prefix");
    public static Color EmbedColour = Color.decode("#1E90FF");
    public static Long BotOwnerID = 0L;

    public static final Logger LOG = LogManager.getLogger("JukeBot");

    public static JDABuilder builder = new JDABuilder(AccountType.BOT)
            .setToken(db.getPropertyFromConfig("token"))
            .setReconnectQueue(new SessionReconnectQueue())
            .addEventListener(waiter, new EventListener())
            .setAudioSendFactory(new NativeAudioSendFactory())
            .setGame(Game.of(defaultPrefix + "help | jukebot.xyz"));

    public static void Configure() {
        Thread.currentThread().setName("JukeBot-Main");
        String color = db.getPropertyFromConfig("color");
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

    }

}
