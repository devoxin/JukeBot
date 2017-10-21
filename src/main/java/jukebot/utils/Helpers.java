package jukebot.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.managers.AudioManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Helpers {
    private static ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "JukeBot-Helper"));
    private static Permissions permissions = new Permissions();

    private static int DURATION_LIMIT_NORMAL = 8000; // 2 hours
    private static int DURATION_LIMIT_PREMIUM = 20000; // 5 hours

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


    public static QUEUE_STATUS CanQueue(AudioTrack track, long userID) {
        int permissionLevel = permissions.getTierLevel(userID);

        if (track.getInfo().isStream && permissionLevel < 1)
            return QUEUE_STATUS.IS_STREAM;

        if (!track.getInfo().isStream) {
            if (track.getDuration() / 1000 > DURATION_LIMIT_NORMAL && permissionLevel < 1)
                return QUEUE_STATUS.EXCEEDS_DURATION;
            if (track.getDuration() / 1000 > DURATION_LIMIT_PREMIUM)
                return QUEUE_STATUS.EXCEEDS_DURATION;
        }

        return QUEUE_STATUS.CAN_QUEUE;
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

    public enum QUEUE_STATUS {
        CAN_QUEUE,
        IS_STREAM,
        EXCEEDS_DURATION
    }
}
