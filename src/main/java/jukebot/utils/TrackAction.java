package jukebot.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.audioutilities.GuildMusicManager;
import net.dv8tion.jda.core.entities.Message;

import java.util.List;

public class TrackAction {

    public final Message m;
    public final List<AudioTrack> tracks;
    public final GuildMusicManager manager;

    public TrackAction(Message m, List<AudioTrack> tracks, GuildMusicManager manager) {
        this.m = m;
        this.tracks = tracks;
        this.manager = manager;
    }
}
