package jukebot.audioutilities;

import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import jukebot.JukeBot;
import jukebot.utils.Helpers;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.audio.AudioSendHandler;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.managers.AudioManager;

import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class AudioHandler extends AudioEventAdapter implements AudioSendHandler {

    private final Permissions permissions = new Permissions();

    public AudioPlayer player;
    public EqualizerFactory equalizer;
    private AudioFrame lastFrame;
    private final Random selector = new Random();

    private final LinkedList<AudioTrack> queue = new LinkedList<>();
    private final LinkedList<Long> skipVotes = new LinkedList<>();
    private Long channelId;
    private boolean shouldAnnounce = true;

    private repeatMode repeat = repeatMode.NONE;
    private boolean shuffle = false;

    private AudioTrack current = null;

    public int trackPacketLoss = 0;
    public int trackPackets = 0;

    public AudioHandler(AudioPlayer player, EqualizerFactory equalizer) {
        this.player = player;
        this.equalizer = equalizer;
        player.addListener(this);
        player.setFilterFactory(equalizer);
    }

    /*
     * Custom Events
     */

    boolean addToQueue(AudioTrack track, Long userID) { // boolean: shouldAnnounce
        track.setUserData(userID);

        if (!player.startTrack(track, true)) {
            queue.offer(track);
            return true;
        }

        return false;
    }

    public void stop() {
        if (isPlaying()) {
            queue.clear();
            playNext();
        }
    }

    public boolean isPlaying() {
        return player.getPlayingTrack() != null;
    }

    public LinkedList<AudioTrack> getQueue() {
        return queue;
    }

    public String getRepeatMode() {
        return repeat.toString().toLowerCase();
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
        if (!skipVotes.contains(userID))
            skipVotes.add(userID);
        return skipVotes.size();
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
            repeat = repeatMode.NONE;
            shuffle = false;
            current = null;
            equalizer.setGain(0, 0F); // Reset any bass boosts
            equalizer.setGain(0, 0F);
            player.stopTrack();

            final TextChannel channel = JukeBot.shardManager.getTextChannelById(channelId);
            if (channel == null) return;

            final AudioManager audioManager = channel.getGuild().getAudioManager();

            if (audioManager.isConnected() || audioManager.isAttemptingToConnect()) {
                Helpers.schedule(audioManager::closeAudioConnection, 1, TimeUnit.SECONDS);

                announce("Queue Concluded!", "[Support the development of JukeBot!](https://www.patreon.com/Devoxin)");
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
                .setColor(JukeBot.embedColour)
                .setTitle(title)
                .setDescription(description)
                .build()
        ).queue();
    }

    /*
     * Player Events
     */

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        skipVotes.clear();
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

        announce("Track Playback Failed", exception.getLocalizedMessage());
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        if (repeat != repeatMode.NONE)
            repeat = repeatMode.NONE;

        announce("Track Stuck", "JukeBot has automatically detected a stuck track and will now play the next song in the queue.");
        playNext();
    }

    /*
     * Packet Sending Events
     */

    @Override
    public boolean canProvide() {
        lastFrame = player.provide();

        if (!player.isPaused()) {
            if (lastFrame == null)
                trackPacketLoss++;
            else
                trackPackets++;
        }

        return lastFrame != null;
    }

    @Override
    public byte[] provide20MsAudio() {
        return lastFrame.data;
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    public enum repeatMode {
        SINGLE, ALL, NONE
    }

}
