package jukebot.commands;

import jukebot.DatabaseHandler;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Help implements Command {

    private final DatabaseHandler db = new DatabaseHandler();

    public void execute(GuildMessageReceivedEvent e, String query) {

        if (query.contains("-a"))
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .addField("Alias", "p\nq\nfs\nr\nps\nn\nff\nvol\nsel\nuq\nsc", true)
                    .addField("Relative Command", "play\nqueue\nforceskip\nresume\npause\nnow\nfastforward\nvolume\nselect\nunqueue\nscsearch", true)
                    .build()
            ).queue();
        else {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setDescription("[Click here for additional help](http://jukebot-discord.xyz/documentation)\nUse `" + db.getPrefix(e.getGuild().getIdLong()) + "help -a` to view command aliases.")
                    .addField("Playback Control", "play\npause\nstop\nresume\nskip\nforceskip\nselect\nscsearch", true)
                    .addField("General Media", "queue\nunqueue\nnow\nshuffle\nfastforward\nvolume\nsave\nrepeat", true)
                    .addField("Misc", "debug\ninvite\nhelp\ndonators\npatreon\nprefix\nreset", true)
                    .setFooter("JukeBot v" + Bot.VERSION, null)
                    .build()
            ).queue();
        }

    }

}