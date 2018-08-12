package jukebot.commands;

import jukebot.Database;
import jukebot.JukeBot;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;

@CommandProperties(description = "Developer menu", category = CommandProperties.category.MISC, developerOnly = true)
public class Dev implements Command {

    public void execute(final Context context) {

        final String[] args = context.getArgs();

        if (args[0].equalsIgnoreCase("preload")) {
            if (JukeBot.isSelfHosted) {
                context.sendEmbed("Command Unavailable", "This command is unavailable on self-hosted JukeBot.");
                return;
            }
            if (args.length < 2) {
                context.sendEmbed("Missing Required Arg", "You need to specify `key`");
            } else {
                JukeBot.createPatreonApi(args[1]);
                context.getMessage().addReaction("\uD83D\uDC4C").queue();
            }
        } else if (args[0].equalsIgnoreCase("block")) {
            if (args.length < 2) {
                context.sendEmbed("Missing Required Arg", "You need to specify `userId`");
            } else {
                Database.blockUser(Long.parseLong(args[1]));
                context.sendEmbed("User Blocked", args[1] + " is now blocked from using JukeBot.");
            }
        } else if (args[0].equalsIgnoreCase("unblock")) {
            if (args.length < 2) {
                context.sendEmbed("Missing Required Arg", "You need to specify `userId`");
            } else {
                Database.unblockUser(Long.parseLong(args[1]));
                context.sendEmbed("User Unblocked", args[1] + " can now use JukeBot.");
            }
        } else {
            context.sendEmbed("Dev Subcommands", "`->` preload <key>\n`->` block <userId>\n`->` unblock <userId>");
        }
    }

}
