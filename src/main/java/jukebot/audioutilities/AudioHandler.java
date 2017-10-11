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

    private ArrayList<AudioTrack> queue = new ArrayList<>();
    private ArrayList<String> skipVotes = new ArrayList<>();
    private TextChannel channel;

    public Bot.REPEATMODE repeat = Bot.REPEATMODE.NONE;
    public boolean shuffle = false;

    private boolean playNextCalled = false;
    public boolean isResetting = false;
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

    public boolean voteSkip(String userID) {
        if (this.skipVotes.contains(userID))
            return false;
        this.skipVotes.add(userID);
        return true;
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

            if (this.shuffle && (this.repeat == Bot.REPEATMODE.SINGLE || track == null)) {
                if (!this.queue.isEmpty())
                    nextTrack = this.queue.remove(new Random().nextInt(this.queue.size()));

            } else if (track != null && this.repeat == Bot.REPEATMODE.SINGLE) {
                nextTrack = track.makeClone();
                nextTrack.setUserData(track.getUserData());

            } else {
                if (!this.queue.isEmpty())
                    nextTrack = this.queue.remove(0);
            }

            if (nextTrack != null) {
                this.player.startTrack(nextTrack, false);
                LOG.debug("Playing next track in " + this.channel.getGuild().getId());
            } else {
                LOG.debug("Queue ended in " + this.channel.getGuild().getId());
                this.player.stopTrack();
                this.player.setVolume(100);
                if (permissions.canPost(this.channel) && !isResetting) {
                    this.channel.sendMessage(new EmbedBuilder()
                            .setColor(Bot.EmbedColour)
                            .setTitle("Queue Concluded!")
                            .setDescription("[Support JukeBot and receive some awesome benefits!](https://www.patreon.com/Devoxin)")
                            .build()
                    ).queue(null, e -> LOG.warn("Failed to post 'QUEUE_END' message to channel " + this.channel.getId()));
                }
                isResetting = false;
                LOG.debug("Terminating AudioConnection in " + this.channel.getGuild().getId());
                Helpers.DisconnectVoice(this.channel.getGuild().getAudioManager());
                this.repeat = Bot.REPEATMODE.NONE;
                this.shuffle = false;
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
        if (this.repeat == Bot.REPEATMODE.ALL)
            this.queue(track.makeClone(), (long) track.getUserData());

        this.skipVotes.clear();
        if (!playNextCalled)
            playNext(this.repeat == Bot.REPEATMODE.SINGLE ? track : null);
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
        lastFrame = this.player.provide();
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

    public enum TRACK_STATUS {
        PLAYING,
        QUEUED,
        LIMITED
    }

}
