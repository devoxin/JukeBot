package jukebot;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;

public class Shard {

    public JDA jda;

    Shard(int shardId, int totalShards) {
        JukeBot.LOG.info("[" + (shardId + 1) + "/" + totalShards + "] Logging in...");
        try {
            this.jda = new JDABuilder(AccountType.BOT)
                    .setToken(Database.getPropertyFromConfig("token"))
                    .setReconnectQueue(JukeBot.sesh)
                    .addEventListener(JukeBot.waiter, new EventListener())
                    .setAudioSendFactory(new NativeAudioSendFactory())
                    .setGame(Game.of(Game.GameType.LISTENING, JukeBot.defaultPrefix + "help | jukebot.xyz"))
                    .useSharding(shardId, totalShards)
                    .buildAsync();
        } catch (LoginException | RateLimitedException ignored) {
            JukeBot.LOG.error("[" + (shardId + 1) + "/" + totalShards + "] Failed to login");
        }
    }
}
