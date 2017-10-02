package jukebot.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.managers.AudioManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static jukebot.utils.Bot.LOG;

public class Helpers {
    private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static Permissions permissions = new Permissions();

    private static int DURATION_LIMIT_NORMAL = 8000; // 2 hours
    private static int DURATION_LIMIT_PREMIUM = 20000; // 5 hours

    public static void ScheduleClose(AudioManager manager) {
        if (!manager.isConnected() && !manager.isAttemptingToConnect())
            return;

        executor.execute(() -> {
            manager.closeAudioConnection();
            LOG.debug("Terminated AudioConnection in " + manager.getGuild().getId());
        });
    }

    public static QUEUE_STATUS CanQueue(AudioTrack track, String userID) {
        int permissionLevel = permissions.getTierLevel(userID);

        if (track.getInfo().isStream && permissionLevel < 1)
            return QUEUE_STATUS.IS_STREAM;

        if (!track.getInfo().isStream) {
            if (track.getDuration() / 1000 > DURATION_LIMIT_NORMAL && permissionLevel < 1)
                return QUEUE_STATUS.EXCEEDS_DURATION;
            if (track.getDuration() / 1000 > DURATION_LIMIT_PREMIUM && permissionLevel < 2)
                return QUEUE_STATUS.EXCEEDS_DURATION;
        }

        return QUEUE_STATUS.CAN_QUEUE;

    }

    public enum QUEUE_STATUS {
        CAN_QUEUE,
        IS_STREAM,
        EXCEEDS_DURATION
    }
}
