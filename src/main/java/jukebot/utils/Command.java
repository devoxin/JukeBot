package jukebot.utils;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public interface Command {

    void execute(MessageReceivedEvent e, String query);

}
