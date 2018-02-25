package jukebot.commands;

import jukebot.Database;
import jukebot.JukeBot;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.regex.Pattern;

@CommandProperties(description = "Change the current server prefix", category = CommandProperties.category.MISC)
public class Prefix implements Command {

    private final Pattern mentionRegex = Pattern.compile("<@!?\\d{17,20}>");
    private final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        final String currentPrefix = Database.getPrefix(e.getGuild().getIdLong());

        if (query.length() == 0) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Server Prefix")
                    .setDescription("Current prefix: [**" + currentPrefix + "**]()\nChange prefix: [**" + currentPrefix  + "prefix !**]()")
                    .build()
            ).queue();
        } else {
            if (!permissions.isElevatedUser(e.getMember(), false)) {
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(JukeBot.embedColour)
                        .setTitle("Permission Error")
                        .setDescription("You need to have the DJ role.")
                        .build()
                ).queue();
                return;
            }

            final String prefix = query.split("\\s+")[0].trim();

            if (mentionRegex.matcher(prefix).matches()) {
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(JukeBot.embedColour)
                        .setTitle("Invalid Prefix")
                        .setDescription("You cannot set a mention as the prefix.")
                        .build()
                ).queue();
                return;
            }

            final boolean updatedPrefix = Database.setPrefix(e.getGuild().getIdLong(), prefix);
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Server Prefix")
                    .setDescription(updatedPrefix ? "Prefix updated to " + prefix : "Prefix update failed")
                    .build()
            ).queue();
        }

    }
}
