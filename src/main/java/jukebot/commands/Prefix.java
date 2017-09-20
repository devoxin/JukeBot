package jukebot.commands;

import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.DatabaseHandler;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.awt.Color;

public class Prefix implements Command {

    private final DatabaseHandler db = new DatabaseHandler();
    private final Permissions permissions = new Permissions();

    public void execute(MessageReceivedEvent e, String query) {

        if (query.length() == 0) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Server Prefix")
                    .setDescription("You must specify a new prefix. E.g. '" + db.getPrefix(e.getGuild().getIdLong()) + "prefix !'")
                    .build()
            ).queue();
        } else {

            if (!permissions.isElevatedUser(e.getMember(), false)) {
                e.getTextChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Permission Error")
                        .setDescription("You need to have the DJ role!")
                        .build()
                ).queue();
                return;
            }

            final boolean updatedPrefix = db.setPrefix(e.getGuild().getIdLong(), query.split(" ")[0].trim());
            if (updatedPrefix)
                e.getTextChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Server Prefix")
                        .setDescription("Prefix updated to " + query.split(" ")[0].trim())
                        .build()
                ).queue();
            else
                e.getTextChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Server Prefix")
                        .setDescription("Failed to update prefix")
                        .build()
                ).queue();
        }

    }
}
