package jukebot.utils;

import net.dv8tion.jda.core.managers.AudioManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Helpers {
    private static ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "JukeBot-Helper"));
    private static ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "JukeBot-Timer"));

    public static String PadLeft(String character, String text, int length) {
        if (text.length() == length)
            return text;

        StringBuilder textBuilder = new StringBuilder(text);

        while (textBuilder.length() < length)
                textBuilder.insert(0, character);

        return textBuilder.toString();
    }

    public static int ParseNumber(String num, int def) {
        try {
            return Integer.parseInt(num);
        } catch(Exception e) {
            return def;
        }
    }

    public static void DisconnectVoice(AudioManager manager) {
        if (!manager.isConnected() && !manager.isAttemptingToConnect())
            return;

        executor.execute(manager::closeAudioConnection);
    }

    public static String fTime(double time) {
        time = time / 1000;

        int days    = (int) Math.floor((time % 31536000) / 86400);
        int hours   = (int) Math.floor(((time % 31536000) % 86400) / 3600);
        int minutes = (int) Math.floor((((time % 31536000) % 86400) % 3600) / 60);
        int seconds = (int) Math.round((((time % 31536000) % 86400) % 3600) % 60);

        String sdays    = days    > 9 ? "" + days    : "0" + days;
        String shours   = hours   > 9 ? "" + hours   : "0" + hours;
        String sminutes = minutes > 9 ? "" + minutes : "0" + minutes;
        String sseconds = seconds > 9 ? "" + seconds : "0" + seconds;

        return (days > 0 ? sdays + ":" : "") + ((hours == 0 && days == 0) ? "" : shours + ":") + sminutes + ":" + sseconds;
    }

    public static void CreateDelay(Consumer<Runnable> task, int delay, TimeUnit unit) {
        timer.schedule(() -> task.accept(null), delay, unit);
    }

}
