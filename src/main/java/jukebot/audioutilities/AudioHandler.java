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

import java.util.ArrayList;
import java.util.Random;

import static jukebot.utils.Bot.LOG;

public class AudioHandler extends AudioEventAdapter implements AudioSendHandler {

    private final Permissions permissions = new Permissions();

    private AudioPlayer player;
    private AudioFrame lastFrame;
    private Random selector = new Random();

    private ArrayList<AudioTrack> queue = new ArrayList<>();
    private ArrayList<Long> skipVotes = new ArrayList<>();
    private TextChannel channel;

    public REPEATMODE repeat = REPEATMODE.NONE;
    public boolean shuffle = false;

    private boolean playNextCalled = false;
    private String lastPlayed = "";

    AudioHandler(AudioPlayer player) {
        this.player = player;
    }

    /*
     * Custom Events
     */

    public TRACK_STATUS queue(AudioTrack track, long userID) {
        if (Helpers.CanQueue(track, userID) != Helpers.QUEUE_STATUS.CAN_QUEUE)
            return TRACK_STATUS.LIMITED;

        track.setUserData(userID);

        if (!this.player.startTrack(track, true)) {
            this.queue.add(track);
            return TRACK_STATUS.QUEUED;
        }
        return TRACK_STATUS.PLAYING;
    }

    public ArrayList<AudioTrack> getQueue() {
        return this.queue;
    }

    public boolean voteSkip(Long userID) {
        return !this.skipVotes.contains(userID) && this.skipVotes.add(userID);
    }

    public int getVotes() {
        return this.skipVotes.size();
    }

    public void setChannel(TextChannel channel) {
        this.channel = channel;
    }

    public void setPlayer(AudioPlayer player) {
        this.player = player;
    }

    public void clearQueue() {
        this.queue.clear();
    }

    public void playNext(AudioTrack track) {
        playNextCalled = true;
        try {
            AudioTrack nextTrack = null;

            if (this.repeat == REPEATMODE.SINGLE && track != null) {
                nextTrack = track.makeClone();
                nextTrack.setUserData(track.getUserData());
            } else if (!this.queue.isEmpty()) {
                if (this.shuffle)
                    nextTrack = this.queue.remove(selector.nextInt(this.queue.size()));
                else
                    nextTrack = this.queue.remove(0);
            }

            if (nextTrack != null) {
                this.player.startTrack(nextTrack, false);
            } else {
                this.repeat = REPEATMODE.NONE;
                this.shuffle = false;
                this.lastPlayed = "";
                this.player.stopTrack();
                this.player.setVolume(100);
                if (permissions.canPost(this.channel)) {
                    this.channel.sendMessage(new EmbedBuilder()
                            .setColor(Bot.EmbedColour)
                            .setTitle("Queue Concluded!")
                            .setDescription("[Support JukeBot and receive some awesome benefits!](https://www.patreon.com/Devoxin)")
                            .build()
                    ).queue(null, e -> LOG.warn("Failed to post 'QUEUE_END' message to channel " + this.channel.getId()));
                }
                Helpers.DisconnectVoice(this.channel.getGuild().getAudioManager());
            }
        } finally {
            playNextCalled = false;
        }
    }

    /*
     * Player Events
     */

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        this.skipVotes.clear();

        if (this.repeat == REPEATMODE.ALL)
            this.queue(track.makeClone(), (long) track.getUserData());

        if (!playNextCalled)
            playNext(this.repeat == REPEATMODE.SINGLE ? track : null);
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
                    .setTitle("Unable to Play Track")
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
        return (lastFrame = this.player.provide()) != null;
    }

    @Override
    public byte[] provide20MsAudio() {
        return lastFrame.data;
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    public enum TRACK_STATUS {
        PLAYING,
        QUEUED,
        LIMITED
    }

    public enum REPEATMODE {
        SINGLE,
        ALL,
        NONE
    }

}
