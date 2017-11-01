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

import java.util.LinkedList;
import java.util.Random;

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

    private AudioTrack current = null;

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

        if (!player.startTrack(track, true)) {
            queue.offer(track);
            return true;
        }

        return false;
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

    public void setRepeat(REPEATMODE mode) {
        repeat = mode;
    }

    public int voteSkip(long userID) {
        if (!skipVotes.contains(userID))
            skipVotes.add(userID);
        return skipVotes.size();
    }

    public void setChannel(TextChannel channel) {
        this.channel = channel;
    }

    public void playNext() {
        AudioTrack nextTrack = null;

        if (repeat == REPEATMODE.SINGLE && current != null) {
            nextTrack = current.makeClone();
            nextTrack.setUserData(current.getUserData());
        } if (!queue.isEmpty()) {
            if (shuffle)
                nextTrack = queue.remove(selector.nextInt(queue.size()));
            else
                nextTrack = queue.poll();
        }

        if (nextTrack != null) {
            player.startTrack(nextTrack, false);
        } else {
            resetPlayer();
            Helpers.DisconnectVoice(channel.getGuild().getAudioManager());
            if (permissions.canPost(channel)) {
                channel.sendMessage(new EmbedBuilder()
                        .setColor(JukeBot.EmbedColour)
                        .setTitle("Queue Concluded!")
                        .setDescription("[Support the development of JukeBot!](https://www.patreon.com/Devoxin)")
                        .build()
                ).queue();
            }
        }
    }

    private void resetPlayer() {
        repeat = REPEATMODE.NONE;
        shuffle = false;
        current = null;
        player.stopTrack();
        player.setVolume(100);
    }

    /*
     * Player Events
     */

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        skipVotes.clear();
        trackPacketLoss = 0;
        trackPackets = 0;

        if (repeat == REPEATMODE.ALL)
            addToQueue(track.makeClone(), (long) track.getUserData());

        if (endReason.mayStartNext)
            playNext();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        player.setPaused(false);
        if (permissions.canPost(channel)) {
            if (current != null && current.getIdentifier().equals(track.getIdentifier()))
                return;

            current = track;
            channel.sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Now Playing")
                    .setDescription(track.getInfo().title)
                    .build()
            ).queue();
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        if (repeat != REPEATMODE.NONE)
            repeat = REPEATMODE.NONE;

        if (permissions.canPost(channel)) {
            channel.sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Track Playback Failed")
                    .setDescription(exception.getLocalizedMessage())
                    .build()
            ).queue();
        }
        playNext();
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        if (repeat != REPEATMODE.NONE)
            repeat = REPEATMODE.NONE;

        if (permissions.canPost(channel)) {
            channel.sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
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
        if (lastFrame == null)
            trackPacketLoss++;
        else
            trackPackets++;

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
