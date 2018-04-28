package jukebot.commands;

import jukebot.Database;
import jukebot.JukeBot;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import javax.xml.crypto.Data;

@CommandProperties(description = "Developer menu", category = CommandProperties.category.MISC, developerOnly = true)
public class Dev implements Command {

    public void execute(GuildMessageReceivedEvent e, String query) {

        String[] args = query.split("\\s+");

        if (args[0].equalsIgnoreCase("preload")) {
            if (JukeBot.isSelfHosted) {
                e.getChannel().sendMessage("Command unavailable").queue();
                return;
            }
            if (args.length < 2) {
                e.getChannel().sendMessage("Missing arg `key`").queue();
            } else {
                JukeBot.recreatePatreonApi(args[1]);
                e.getMessage().addReaction("\uD83D\uDC4C").queue();
            }
        } else if (args[0].equalsIgnoreCase("block")) {
            if (args.length < 2) {
                e.getChannel().sendMessage("Missing arg `userId`").queue();
            } else {
                Database.blockUser(Long.parseLong(args[1]));
                e.getChannel().sendMessage("User blocked.").queue();
            }
        } else if (args[0].equalsIgnoreCase("unblock")) {
            if (args.length < 2) {
                e.getChannel().sendMessage("Missing arg `userId`").queue();
            } else {
                Database.unblockUser(Long.parseLong(args[1]));
                e.getChannel().sendMessage("User unblocked.").queue();
            }
        } else {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Debug Subcommands")
                    .setDescription("`->` preload <key>\n`->` block <userId>\n`->` unblock <userId>")
                    .build()
            ).queue();
        }
    }

}
