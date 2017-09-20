package jukebot.commands;

import jukebot.DatabaseHandler;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.awt.*;

public class Manage implements Command {

    private final DatabaseHandler db = new DatabaseHandler();
    private final Permissions permissions = new Permissions();

    public void execute(MessageReceivedEvent e, String query) {

        if (!permissions.isBotOwner(e.getAuthor().getId())) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Manage")
                    .setDescription("Command reserved for bot developer.")
                    .build()
            ).queue();
            return;
        }

        String[] args = query.split(" ");

        if (args[0].equalsIgnoreCase("donators")) {

            if (args.length == 1) {
                e.getTextChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Manage | Donators")
                        .setDescription("Donator ID must not be empty.")
                        .build()
                ).queue();
                return;
            }

            if (args.length == 2) {
                e.getTextChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Manage")
                        .setDescription("Method must not be empty. [get/set]")
                        .build()
                ).queue();
                return;
            }

            if (args[2].equalsIgnoreCase("get")) {

                final String userTier = db.getTier(Long.parseLong(args[1]));
                e.getTextChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Donator Status")
                        .setDescription("User **" + args[1] + "** has Tier **" + userTier + "**")
                        .build()
                ).queue();

            }

            if (args[2].equalsIgnoreCase("set")) {

                if (args.length == 3) {
                    e.getTextChannel().sendMessage(new EmbedBuilder()
                            .setColor(Bot.EmbedColour)
                            .setTitle("Donator Tier")
                            .setDescription("A new tier must be specified")
                            .build()
                    ).queue();
                    return;
                }

                final boolean result = db.setTier(Long.parseLong(args[1]), args[3]);
                e.getTextChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Donator Tier")
                        .setDescription("The user's tier was " + (result ? "updated" : "unchanged"))
                        .build()
                ).queue();

            }

        }

    }
}
