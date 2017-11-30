package jukebot.utils;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public interface Command {

    void execute(final GuildMessageReceivedEvent e, final String query);

    default CommandProperties properties() {
        return this.getClass().isAnnotationPresent(CommandProperties.class)
                ? this.getClass().getAnnotation(CommandProperties.class)
                : null;
    }

    default String name() {
        return this.getClass().getSimpleName();
    }

}