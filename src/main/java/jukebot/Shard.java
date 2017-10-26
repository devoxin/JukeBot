package jukebot;

import jukebot.utils.Bot;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;

import static jukebot.utils.Bot.LOG;

public class Shard {

    public JDA jda;

    Shard(int shardId, int totalShards) {
        LOG.info("[" + (shardId + 1) + "/" + totalShards + "] Logging in...");
        try {
            this.jda = Bot.builder
                    .addEventListener(new EventListener())
                    .useSharding(shardId, totalShards)
                    .buildAsync();
        } catch (LoginException | RateLimitedException ignored) {
            LOG.error("[" + (shardId + 1) + "/" + totalShards + "] Failed to login");
        }
    }
}
