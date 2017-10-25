package jukebot.utils;

public class ConnectionError {

    public String title = "";
    public String description = "";

    ConnectionError(final String title, final String description) {
        this.title = title;
        this.description = description;
    }

}
