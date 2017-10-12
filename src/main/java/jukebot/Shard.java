package jukebot;

import jukebot.utils.Bot;
import net.dv8tion.jda.core.JDA;

import static jukebot.utils.Bot.LOG;

public class Shard {

    public JDA jda;

    Shard(int shardId, int totalShards) throws Exception {
        LOG.info("[" + (shardId + 1) + "/" + totalShards + "] Logging in...");
        this.jda = Bot.builder
                .useSharding(shardId, totalShards)
                .buildAsync();
    }
}
