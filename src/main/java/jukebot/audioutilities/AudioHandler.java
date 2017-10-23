package jukebot.audioutilities;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import jukebot.utils.Bot;
import jukebot.utils.Helpers;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.audio.AudioSendHandler;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.LinkedList;
import java.util.Random;

import static jukebot.utils.Bot.LOG;

public class AudioHandler extends AudioEventAdapter implements AudioSendHandler {

    private final Permissions permissions = new Permissions();

    private AudioPlayer player;
    private AudioFrame lastFrame;
    private final Random selector = new Random();

    private final LinkedList<AudioTrack> queue = new LinkedList<>();
    private final LinkedList<Long> skipVotes = new LinkedList<>();
    private TextChannel channel;

    private REPEATMODE repeat = REPEATMODE.NONE;
    private boolean shuffle = false;

    private String lastPlayed = "";

    public int trackPacketLoss = 0;
    public int trackPackets = 0;

    AudioHandler(AudioPlayer player) {
        this.player = player;
    }

    /*
     * Custom Events
     */

    boolean addToQueue(AudioTrack track, long userID) { // boolean: shouldAnnounce
        track.setUserData(userID);

        if (!this.player.startTrack(track, true)) {
            this.queue.offer(track);
            return true;
        }

        return false;
    }

    public LinkedList<AudioTrack> getQueue() {
        return this.queue;
    }

    public String getStringifiedRepeat() {
        return String.valueOf(this.repeat).toLowerCase();
    }

    public boolean isShuffleEnabled() {
        return this.shuffle;
    }

    public boolean toggleShuffle() {
        return this.shuffle = !this.shuffle;
    }

    public void setRepeat(REPEATMODE mode) {
        this.repeat = mode;
    }

    public int voteSkip(long userID) {
        if (!this.skipVotes.contains(userID))
            this.skipVotes.add(userID);
        return this.skipVotes.size();
    }

    public void setChannel(TextChannel channel) {
        this.channel = channel;
    }

    public void playNext(AudioTrack track) {
        AudioTrack nextTrack = null;

        if (this.repeat == REPEATMODE.SINGLE && track != null) {
            nextTrack = track.makeClone();
            nextTrack.setUserData(track.getUserData());
        } else if (!this.queue.isEmpty()) {
            if (this.shuffle)
                nextTrack = this.queue.remove(selector.nextInt(this.queue.size()));
            else
                nextTrack = this.queue.removeFirst();
        }

        if (nextTrack != null) {
            this.player.startTrack(nextTrack, false);
        } else {
            resetPlayer();
            Helpers.DisconnectVoice(this.channel.getGuild().getAudioManager());
            if (permissions.canPost(this.channel)) {
                this.channel.sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Queue Concluded!")
                        .setDescription("[Support JukeBot and receive some awesome benefits!](https://www.patreon.com/Devoxin)")
                        .build()
                ).queue(null, e -> LOG.warn("Failed to post 'QUEUE_END' message to channel " + this.channel.getId()));
            }
        }
    }

    private void resetPlayer() {
        this.repeat = REPEATMODE.NONE;
        this.shuffle = false;
        this.lastPlayed = "";
        this.player.stopTrack();
        this.player.setVolume(100);
    }

    /*
     * Player Events
     */

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // This event is fired when the track ends naturally
        this.skipVotes.clear();
        this.trackPacketLoss = 0; // Reset Track Performance Statistics
        this.trackPackets = 0;   // Reset Track Performance Statistics

        if (this.repeat == REPEATMODE.ALL)
            this.addToQueue(track.makeClone(), (long) track.getUserData());

        if (endReason.mayStartNext)
            playNext(track);
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        this.player.setPaused(false);
        if (permissions.canPost(this.channel)) {
            if (this.lastPlayed.equals(track.getIdentifier()))
                return;

            this.lastPlayed = track.getIdentifier();
            this.channel.sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Now Playing")
                    .setDescription(track.getInfo().title)
                    .build()
            ).queue(null, e -> LOG.warn("Failed to post 'NOW_PLAYING' message to channel " + this.channel.getId()));
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        if (permissions.canPost(this.channel)) {
            this.channel.sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Track Playback Failed")
                    .setDescription(exception.getLocalizedMessage())
                    .build()
            ).queue(null, e -> LOG.warn("Failed to post 'TRACK_ERROR' message to channel " + this.channel.getId()));
        }
        playNext(null);
    }

    /*
     * Packet Sending Events
     */

    @Override
    public boolean canProvide() {
        lastFrame = this.player.provide();
        if (lastFrame == null)
            this.trackPacketLoss++;
        else
            this.trackPackets++;

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

    public enum REPEATMODE {
        SINGLE, ALL, NONE
    }

}
