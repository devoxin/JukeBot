package jukebot.utils;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public interface Command {

    void execute(GuildMessageReceivedEvent e, String query);

}
