package jukebot.audioutilities;

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
    private AudioFrame lastFrame;
    private final Random selector = new Random();

    private final LinkedList<AudioTrack> queue = new LinkedList<>();
    private final LinkedList<Long> skipVotes = new LinkedList<>();
    private Long channelId;

    private repeatMode repeat = repeatMode.NONE;
    private boolean shuffle = false;

    private AudioTrack current = null;

    public int trackPacketLoss = 0;
    public int trackPackets = 0;

    public AudioHandler(AudioPlayer player) {
        this.player = player;
        player.addListener(this);
    }

    /*
     * Custom Events
     */

    boolean addToQueue(AudioTrack track, long userID) { // boolean: shouldAnnounce
        track.setUserData(userID);

        if (!player.startTrack(track, true)) {
            queue.offer(track);
            return true;
        }

        return false;
    }

    public void stop() {
        queue.clear();
        playNext();
    }

    public boolean isPlaying() {
        return player.getPlayingTrack() != null;
    }

    public LinkedList<AudioTrack> getQueue() {
        return queue;
    }

    public String getStringifiedRepeat() {
        return String.valueOf(repeat).toLowerCase();
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

    public int voteSkip(long userID) {
        if (!skipVotes.contains(userID))
            skipVotes.add(userID);
        return skipVotes.size();
    }

    public void setChannel(Long channelId) {
        this.channelId = channelId;
    }

    public void playNext() {
        AudioTrack nextTrack = null;

        if (repeat == repeatMode.SINGLE && current != null) {
            nextTrack = current.makeClone();
            nextTrack.setUserData(current.getUserData());
        } if (!queue.isEmpty()) {
            nextTrack = shuffle ? queue.remove(selector.nextInt(queue.size())) : queue.poll();
        }

        if (nextTrack != null) {
            player.startTrack(nextTrack, false);
        } else {
            resetPlayer();

            final TextChannel channel = JukeBot.shardManager.getTextChannelById(channelId);
            if (channel == null) return;

            final AudioManager audioManager = channel.getGuild().getAudioManager();

            if (audioManager.isConnected() || audioManager.isAttemptingToConnect()) {
                Helpers.schedule(audioManager::closeAudioConnection, 1, TimeUnit.SECONDS);

                if (permissions.canSendTo(channel)) {
                    channel.sendMessage(new EmbedBuilder()
                            .setColor(JukeBot.embedColour)
                            .setTitle("Queue Concluded!")
                            .setDescription("[Support the development of JukeBot!](https://www.patreon.com/Devoxin)")
                            .build()
                    ).queue();
                }
            }
        }
    }

    private void resetPlayer() {
        repeat = repeatMode.NONE;
        shuffle = false;
        current = null;
        player.stopTrack();
    }

    /*
     * Player Events
     */

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        skipVotes.clear();
        trackPacketLoss = 0;
        trackPackets = 0;

        if (repeat == repeatMode.ALL)
            addToQueue(track.makeClone(), (long) track.getUserData());

        if (endReason.mayStartNext)
            playNext();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        player.setPaused(false);

        final TextChannel channel = JukeBot.shardManager.getTextChannelById(channelId);

        if (channel != null && permissions.canSendTo(channel)) {
            if (current != null && current.getIdentifier().equals(track.getIdentifier()))
                return;

            current = track;
            channel.sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Now Playing")
                    .setDescription(track.getInfo().title)
                    .build()
            ).queue();
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        if (repeat != repeatMode.NONE)
            repeat = repeatMode.NONE;

        final TextChannel channel = JukeBot.shardManager.getTextChannelById(channelId);

        if (channel != null && permissions.canSendTo(channel)) {
            channel.sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Track Playback Failed")
                    .setDescription(exception.getLocalizedMessage())
                    .build()
            ).queue();
        }
        playNext();
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        if (repeat != repeatMode.NONE)
            repeat = repeatMode.NONE;

        final TextChannel channel = JukeBot.shardManager.getTextChannelById(channelId);

        if (channel != null && permissions.canSendTo(channel)) {
            channel.sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Track Stuck")
                    .setDescription("JukeBot has automatically detected a stuck track and will now play the next song in the queue.")
                    .build()
            ).queue();
        }
        playNext();
    }

    /*
     * Packet Sending Events
     */

    @Override
    public boolean canProvide() {
        lastFrame = player.provide();

        if (!player.isPaused()) {
            if (lastFrame == null || lastFrame.data.length == 0)
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
