package jukebot.audioutilities;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import jukebot.utils.Bot;

public class GuildMusicManager {

    public AudioPlayer player;
    public AudioHandler handler;

    public GuildMusicManager() {
        this.player = Bot.playerManager.createPlayer();
        this.handler = new AudioHandler(this.player);
        this.player.addListener(this.handler);
    }

    public void ResetPlayer() {
        this.player.destroy();
        this.player = Bot.playerManager.createPlayer();
        this.handler.setPlayer(this.player);
        this.player.addListener(this.handler);
    }
}
