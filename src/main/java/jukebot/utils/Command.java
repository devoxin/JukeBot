package jukebot.utils;

public interface Command {

    void execute(final Context context);

    default CommandProperties properties() {
        return this.getClass().getAnnotation(CommandProperties.class);
    }

    default String name() {
        return this.getClass().getSimpleName();
    }

}