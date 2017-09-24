package jukebot.utils;

import net.dv8tion.jda.core.managers.AudioManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static jukebot.utils.Bot.LOG;

public class Helpers {

    public static void ScheduleClose(AudioManager manager) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            manager.closeAudioConnection();
            LOG.debug("Terminated AudioConnection in " + manager.getGuild().getId());
            executor.shutdown();
        }, 1, TimeUnit.SECONDS);
    }

}
