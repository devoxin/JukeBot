package jukebot.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.ActionWaiter;
import jukebot.audioutilities.GuildMusicManager;
import net.dv8tion.jda.core.entities.Message;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TrackAction {

    public final Message m;
    public final List<AudioTrack> tracks;
    public final GuildMusicManager manager;
    public final ScheduledExecutorService waiter = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "JukeBot-SelectionWaiter"));

    public TrackAction(Message m, List<AudioTrack> tracks, GuildMusicManager manager, Long UserID) {
        this.m = m;
        this.tracks = tracks;
        this.manager = manager;

        waiter.schedule(() -> {
            if (ActionWaiter.UserManagers.containsKey(UserID)) {
                ActionWaiter.UserManagers.remove(UserID);
                m.delete().queue();
            }
            waiter.shutdown();
        }, 10, TimeUnit.SECONDS);
    }
}
