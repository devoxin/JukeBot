package jukebot.commands;

import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(description = "Removes the last song queued by you", category = CommandProperties.category.MEDIA)
public class Undo implements Command {

    @Override
    public void execute(GuildMessageReceivedEvent e, String query) {
        e.getChannel().sendMessage("execute order 66").queue();
    }
}
