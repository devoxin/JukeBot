package jukebot.utils;

import net.dv8tion.jda.core.managers.AudioManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static jukebot.utils.Bot.LOG;

public class Helpers {
    private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public static void ScheduleClose(AudioManager manager) {
        executor.execute(() -> {
            manager.closeAudioConnection();
            LOG.debug("Terminated AudioConnection in " + manager.getGuild().getId());
        });
    }

}
