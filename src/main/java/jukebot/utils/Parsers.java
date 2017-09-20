package jukebot.utils;

public class Parsers {

    public static int Number(String num, int def) {
        try {
            return Integer.parseInt(num);
        } catch(Exception e) {
            return def;
        }
    }

}
