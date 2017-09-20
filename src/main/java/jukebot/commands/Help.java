package jukebot.commands;

import jukebot.DatabaseHandler;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.awt.*;

public class Help implements Command {

    private final DatabaseHandler db = new DatabaseHandler();

    public void execute(MessageReceivedEvent e, String query) {

        if (query.contains("-a"))
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .addField("Alias", "p\nq\nfs\nr\nps\nn\nff\nvol\nsel\nuq", true)
                    .addField("Relative Command", "play\nqueue\nforceskip\nresume\npause\nnow\nfastforward\nvolume\nselect\nunqueue", true)
                    .build()
            ).queue();
        else {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setDescription("[Click here for additional help](http://jukebot-discord.xyz/documentation)\nUse `" + db.getPrefix(e.getGuild().getIdLong()) + "help -a` to view command aliases.")
                    .addField("Playback Control", "play\npause\nstop\nresume\nskip\nforceskip\nselect", true)
                    .addField("General Media", "queue\nunqueue\nnow\nshuffle\nfastforward\nvolume\nsave\nrepeat", true)
                    .addField("Misc", "debug\ninvite\nhelp\nmanage\npatreon\nprefix\nreset", true)
                    .build()
            ).queue();
        }

    }

}