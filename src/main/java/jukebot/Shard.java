package jukebot;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import jukebot.utils.Bot;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Game;

public class Shard {

    public JDA jda;

    Shard(int shardId, int totalShards) throws Exception {
        Bot.Log("[" + (shardId + 1) + "/" + totalShards + "] Logging in...", Bot.LOGTYPE.INFORMATION);
        this.jda = Bot.builder
                .setAudioSendFactory(new NativeAudioSendFactory())
                .addEventListener(new EventListener())
                .useSharding(shardId, totalShards)
                .setGame(Game.of(Bot.defaultPrefix + "help | " + Bot.VERSION + " | [" + (shardId + 1) + "/" + totalShards + "]"))
                .buildAsync();
    }
}
