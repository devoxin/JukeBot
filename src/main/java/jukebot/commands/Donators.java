package jukebot.commands;

import jukebot.DatabaseHandler;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.HashMap;
import java.util.List;

public class Donators implements Command {

    private final DatabaseHandler db = new DatabaseHandler();
    private final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        if (!permissions.isBotOwner(e.getAuthor().getIdLong())) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Donators")
                    .setDescription("Command reserved for bot developer.")
                    .build()
            ).queue();
            return;
        }

        String[] args = query.split(" ");

        if (args.length == 0) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Donators")
                    .setDescription("<getall|get|set> [id] [tier]")
                    .build()
            ).queue();
            return;
        }

        if (args.length != 3) {
            if ("getall".equalsIgnoreCase(args[0])) {
                final HashMap<Long, String> donators = db.getAllDonators();
                if (donators == null || donators.isEmpty()) {
                    e.getChannel().sendMessage(new EmbedBuilder()
                            .setColor(Bot.EmbedColour)
                            .setTitle("Donators")
                            .setDescription("No donators returned.")
                            .build()
                    ).queue();
                    return;
                }

                StringBuilder t1 = new StringBuilder().append("\u200B"); // Fail-safe in case no donators exist in this tier
                StringBuilder t2 = new StringBuilder().append("\u200B"); // Fail-safe in case no donators exist in this tier
                StringBuilder t3 = new StringBuilder().append("\u200B"); // Fail-safe in case no donators exist in this tier

                for (HashMap.Entry<Long, String> entry : donators.entrySet()) {
                    User donator = e.getJDA().getUserById(entry.getKey());
                    if (donator == null)
                        donator = e.getJDA().retrieveUserById(entry.getKey()).complete();

                    if ("1".equalsIgnoreCase(entry.getValue())) {
                        t1.append("`")
                                .append(entry.getKey()).append("` ")
                                .append(donator.getName())
                                .append("\n");
                    } else if ("2".equalsIgnoreCase(entry.getValue())) {
                        t2.append("`")
                                .append(entry.getKey()).append("` ")
                                .append(donator.getName())
                                .append("\n");
                    } else if ("3".equalsIgnoreCase(entry.getValue())) {
                        t3.append("`")
                                .append(entry.getKey()).append("` ")
                                .append(donator.getName())
                                .append("\n");
                    }
                }

                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .addField("Tier 1", t1.toString(), true)
                        .addField("Tier 2", t2.toString(), true)
                        .addField("Tier 3", t3.toString(), true)
                        .build()
                ).queue();

            } else {
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Donators")
                        .setDescription("<getall|get|set> [id] [tier]")
                        .build()
                ).queue();
            }
        } else {
            if ("get".equalsIgnoreCase(args[0])) {
                final String userTier = db.getTier(Long.parseLong(args[1]));
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Donator Status")
                        .setDescription("User **" + args[1] + "** has Tier **" + userTier + "**")
                        .build()
                ).queue();
            }

            if ("set".equalsIgnoreCase(args[0])) {
                final boolean result = db.setTier(Long.parseLong(args[1]), args[2]);
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Donator Tier")
                        .setDescription("The user's tier was " + (result ? "updated" : "unchanged"))
                        .build()
                ).queue();
            }
        }

    }
}
