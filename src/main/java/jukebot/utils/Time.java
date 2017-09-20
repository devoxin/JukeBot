package jukebot.utils;

public class Time {

    public static String format(double time) {
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
}
