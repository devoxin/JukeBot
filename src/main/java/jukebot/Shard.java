package jukebot;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import jukebot.utils.Bot;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Game;

import static jukebot.utils.Bot.LOG;

public class Shard {

    public JDA jda;

    Shard(int shardId, int totalShards) throws Exception {
        LOG.info("[" + (shardId + 1) + "/" + totalShards + "] Logging in...");
        this.jda = Bot.builder
                .setAudioSendFactory(new NativeAudioSendFactory())
                .addEventListener(new EventListener())
                .useSharding(shardId, totalShards)
                .setGame(Game.of(Bot.defaultPrefix + "help | " + Bot.VERSION))
                .buildAsync();
    }
}
