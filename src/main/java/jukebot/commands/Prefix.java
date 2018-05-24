package jukebot.commands;

import jukebot.Database;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;

import java.util.regex.Pattern;

@CommandProperties(description = "Change the current server prefix", category = CommandProperties.category.MISC)
public class Prefix implements Command {

    private final Pattern mentionRegex = Pattern.compile("<@!?\\d{17,20}>");

    public void execute(final Context context) {

        final String currentPrefix = context.getPrefix();
        final String newPrefix = context.getArgString();

        if (newPrefix.isEmpty()) {
            context.sendEmbed("Server Prefix", "Current prefix: **" + currentPrefix + "**\nChange prefix: **" + currentPrefix + "prefix !**");
            return;
        }

        if (!context.isDJ(false)) {
            context.sendEmbed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.xyz/faq)");
            return;
        }

        if (mentionRegex.matcher(newPrefix).matches()) {
            context.sendEmbed("Invalid Prefix", "Mentions cannot be used as prefixes.");
            return;
        }

        final Boolean updatedPrefix = Database.setPrefix(context.getGuild().getIdLong(), newPrefix);
        final String resp = updatedPrefix ? "Prefix updated to `" + newPrefix + "`" : "Failed to update prefix!";

        context.sendEmbed("Server Prefix", resp);

    }
}
