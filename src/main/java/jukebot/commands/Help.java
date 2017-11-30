package jukebot.commands;

import jukebot.Database;
import jukebot.EventListener;
import jukebot.JukeBot;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Helpers;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.Comparator;

@CommandProperties(description = "Displays all commands", aliases = {"commands"}, category = CommandProperties.category.MISC)
public class Help implements Command {

    public void execute(GuildMessageReceivedEvent e, String query) {

        switch (query) {
            case "1":
                e.getChannel().sendMessage(CreateHelpEmbed(
                        "You can use the **play** command to make JukeBot join your channel, search for the specified song and begin playing.\n`"
                                + Database.getPrefix(e.getGuild().getIdLong()) + "play <URL/Search Query>`"
                )).queue();
                break;

            case "2":
                final StringBuilder controls = new StringBuilder();

                EventListener.commands.values()
                        .stream()
                        .filter(command -> command.properties().category() == CommandProperties.category.CONTROLS)
                        .sorted(Comparator.comparing(Command::name))
                        .forEach(command ->
                                controls.append("**`")
                                        .append(Helpers.padRight(" ", command.name().toLowerCase(), 14))
                                        .append(":`** ")
                                        .append(command.properties().description())
                                        .append("\n")
                        );

                e.getChannel().sendMessage(CreateHelpEmbed(controls.toString())).queue();
                break;

            case "3":
                final StringBuilder media = new StringBuilder();

                EventListener.commands.values()
                        .stream()
                        .filter(command -> command.properties().category() == CommandProperties.category.MEDIA)
                        .sorted(Comparator.comparing(Command::name))
                        .forEach(command ->
                                media.append("**`")
                                        .append(Helpers.padRight(" ", command.name().toLowerCase(), 14))
                                        .append(":`** ")
                                        .append(command.properties().description())
                                        .append("\n")
                        );

                e.getChannel().sendMessage(CreateHelpEmbed(media.toString())).queue();
                break;

            case "4":
                final StringBuilder misc = new StringBuilder();

                EventListener.commands.values()
                        .stream()
                        .filter(command -> command.properties().category() == CommandProperties.category.MISC)
                        .sorted(Comparator.comparing(Command::name))
                        .forEach(command ->
                                misc.append("**`")
                                        .append(Helpers.padRight(" ", command.name().toLowerCase(), 14))
                                        .append(":`** ")
                                        .append(command.properties().description())
                                        .append("\n")
                        );

                e.getChannel().sendMessage(CreateHelpEmbed(misc.toString())).queue();
                break;

            default:
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(JukeBot.EmbedColour)
                        .setTitle("Help Categories")
                        .setDescription("`1.` Getting Started\n`2.` Controls\n`3.` Media\n`4.` Miscellaneous\n\nUse `" + Database.getPrefix(e.getGuild().getIdLong()) + "help <number>` to select a category")
                        .build()
                ).queue();
        }
    }


    private MessageEmbed CreateHelpEmbed(String description) {
        return new EmbedBuilder()
                .setColor(JukeBot.EmbedColour)
                .setDescription("[View more information here](http://jukebot.xyz/documentation)\n" + description)
                .build();
    }

}