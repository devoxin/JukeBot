package jukebot.commands;

import jukebot.DatabaseHandler;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Prefix implements Command {

    private final DatabaseHandler db = new DatabaseHandler();
    private final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        if (query.length() == 0) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Server Prefix")
                    .setDescription("You must specify a new prefix. E.g. `" + db.getPrefix(e.getGuild().getIdLong()) + "prefix !`")
                    .build()
            ).queue();
        } else {

            if (!permissions.isElevatedUser(e.getMember(), false)) {
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Permission Error")
                        .setDescription("You need to have the DJ role!")
                        .build()
                ).queue();
                return;
            }

            final boolean updatedPrefix = db.setPrefix(e.getGuild().getIdLong(), query.split(" ")[0].trim());
            if (updatedPrefix)
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Server Prefix")
                        .setDescription("Prefix updated to " + query.split(" ")[0].trim())
                        .build()
                ).queue();
            else
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Server Prefix")
                        .setDescription("Failed to update prefix")
                        .build()
                ).queue();
        }

    }
}
