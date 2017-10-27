package jukebot.commands;

import jukebot.Database;
import jukebot.JukeBot;
import jukebot.utils.Command;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Prefix implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        if (query.length() == 0) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Server Prefix")
                    .setDescription("You must specify a new prefix. E.g. `" + Database.getPrefix(e.getGuild().getIdLong()) + "prefix !`")
                    .build()
            ).queue();
        } else {
            if (!permissions.isElevatedUser(e.getMember(), false)) {
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(JukeBot.EmbedColour)
                        .setTitle("Permission Error")
                        .setDescription("You need to have the DJ role.")
                        .build()
                ).queue();
                return;
            }

            final String prefix = query.split(" ")[0].trim();
            final boolean updatedPrefix = Database.setPrefix(e.getGuild().getIdLong(), prefix);
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Server Prefix")
                    .setDescription(updatedPrefix ? "Prefix updated to " + prefix : "Prefix update failed")
                    .build()
            ).queue();
        }

    }
}
