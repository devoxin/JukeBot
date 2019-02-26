package jukebot.audio;

import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import jukebot.Database;
import jukebot.JukeBot;
import jukebot.utils.Helpers;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.audio.AudioSendHandler;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.managers.AudioManager;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class AudioHandler extends AudioEventAdapter implements AudioSendHandler {

    private final Permissions permissions = new Permissions();

    public AudioPlayer player;
    private EqualizerFactory equalizer = new EqualizerFactory();

    private AudioFrame lastFrame;
    private final Random selector = new Random();
    private bassBoost bassBoostMode = AudioHandler.bassBoost.OFF;

    private final LinkedList<AudioTrack> queue = new LinkedList<>();
    private final HashSet<Long> skips = new HashSet<>();
    private Long channelId;
    private Long guildId;
    private boolean shouldAnnounce = true;

    private repeatMode repeat = repeatMode.NONE;
    private boolean shuffle = false;

    private AudioTrack current = null;

    public int trackPacketLoss = 0;
    public int trackPackets = 0;

    public AudioHandler(Long guildId, AudioPlayer player) {
        this.guildId = guildId;
        this.player = player;
        player.addListener(this);
    }

    public boolean enqueue(AudioTrack track, Long userID) { // boolean: shouldAnnounce
        track.setUserData(userID);

        if (!player.startTrack(track, true)) {
            queue.offer(track);
            return true;
        }

        return false;
    }

    public boolean isPlaying() {
        return player.getPlayingTrack() != null;
    }

    public boolean isBassBoosted() {
        return bassBoostMode != bassBoost.OFF;
    }

    public LinkedList<AudioTrack> getQueue() {
        return queue;
    }

    public String getRepeatMode() {
        return repeat.toString().toLowerCase();
    }

    public String getBassBoostSetting() {
        return bassBoostMode.toString().toLowerCase();
    }

    public boolean isShuffleEnabled() {
        return shuffle;
    }

    public boolean toggleShuffle() {
        return shuffle = !shuffle;
    }

    public void setRepeat(repeatMode mode) {
        repeat = mode;
    }

    public int voteSkip(Long userID) {
        skips.add(userID);
        return skips.size();
    }

    public void setChannel(Long channelId) {
        this.channelId = channelId;
    }

    public void setShouldAnnounce(Boolean shouldAnnounce) {
        this.shouldAnnounce = shouldAnnounce;
    }

    public void playNext() {
        AudioTrack nextTrack = null;

        if (repeat == repeatMode.ALL && current != null) {
            AudioTrack r = current.makeClone();
            r.setUserData(current.getUserData());
            queue.offer(r);
        }

        if (repeat == repeatMode.SINGLE && current != null) {
            nextTrack = current.makeClone();
            nextTrack.setUserData(current.getUserData());
        } else if (!queue.isEmpty()) {
            nextTrack = shuffle ? queue.remove(selector.nextInt(queue.size())) : queue.poll();
        }

        if (nextTrack != null) {
            player.startTrack(nextTrack, false);
        } else {
            current = null;
            player.stopTrack();
            bassBoost(bassBoost.OFF);

            final Guild guild = JukeBot.shardManager.getGuildById(guildId);

            if (guild == null) { // Bot was kicked or something
                JukeBot.removePlayer(guildId);
                return;
            }

            final AudioManager audioManager = guild.getAudioManager();

            if (audioManager.isConnected() || audioManager.isAttemptingToConnect()) {
                Helpers.Companion.schedule(audioManager::closeAudioConnection, 1, TimeUnit.SECONDS);

                announce("Queue Concluded!", "[Support the development of JukeBot!](https://www.patreon.com/Devoxin)" +
                        "\nSuggest features with the `feedback` command!");
            }
        }
    }

    private void announce(String title, String description) {
        if (!shouldAnnounce || channelId == null) {
            return;
        }

        final TextChannel channel = JukeBot.shardManager.getTextChannelById(channelId);

        if (channel == null || !permissions.canSendTo(channel)) {
            return;
        }

        channel.sendMessage(new EmbedBuilder()
                .setColor(Database.getColour(channel.getGuild().getIdLong()))
                .setTitle(title)
                .setDescription(truncate(description, 1000))
                .build()
        ).queue(null, err -> JukeBot.LOG.error("Encountered an error while posting track announcement", err));
    }

    private String truncate(String content, int maxLength) {
        if (content.length() > maxLength) {
            return content.substring(0, maxLength - 3) + "...";
        }

        return content;
    }

    public void cleanup() {
        queue.clear();
        skips.clear();
        player.destroy();

        final Guild g = JukeBot.shardManager.getGuildById(guildId);
        if (g != null) {
            g.getAudioManager().setSendingHandler(null);
        }
    }

    public void bassBoost(bassBoost preset) {
        if (preset != bassBoost.OFF && bassBoostMode == bassBoost.OFF) {
            player.setFilterFactory(equalizer);
        } else if (preset == bassBoost.OFF && bassBoostMode != bassBoost.OFF) {
            player.setFilterFactory(null);
        }

        bassBoostMode = preset;
        equalizer.setGain(0, preset.band0);
        equalizer.setGain(1, preset.band1);
        equalizer.setGain(2, preset.band2);
    }

    /*
     * Player Events
     */

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        skips.clear();
        trackPacketLoss = 0;
        trackPackets = 0;

        if (endReason.mayStartNext)
            playNext();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        player.setPaused(false);

        if (current == null || !current.getIdentifier().equals(track.getIdentifier())) {
            current = track;
            announce("Now Playing", track.getInfo().title);
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        if (repeat != repeatMode.NONE)
            repeat = repeatMode.NONE;

        announce("Playback Error", "Playback of **" + track.getInfo().title + "** encountered an error!\n" +
                exception.getLocalizedMessage());
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        if (repeat != repeatMode.NONE)
            repeat = repeatMode.NONE;

        announce("Track Stuck", "Playback of **" + track.getInfo().title + "** has frozen and is unable to resume!");
        playNext();
    }

    /*
     * Packet Sending Events
     */

    @Override
    public boolean canProvide() {
        lastFrame = player.provide();

        if (!player.isPaused()) {
            if (lastFrame == null) {
                trackPacketLoss++;
            } else {
                trackPackets++;
            }
        }

        return lastFrame != null;
    }

    @Override
    public byte[] provide20MsAudio() {
        return lastFrame.getData();
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    public enum repeatMode {
        SINGLE, ALL, NONE
    }

    public enum bassBoost {
        OFF(0F, 0F, 0F),
        WEAK(0.03F, 0.01F, 0.0F),
        MEDIUM(0.1F, 0.08F, 0.04F),
        STRONG(0.2F, 0.15F, 0.11F),
        INSANE(0.4F, 0.26F, 0.18F),
        WTF(1F, 0.8F, 0.6F);

        private final float band0;
        private final float band1;
        private final float band2;

        bassBoost(float b0, float b1, float b2) {
            this.band0 = b0;
            this.band1 = b1;
            this.band2 = b2;
        }
    }

}
