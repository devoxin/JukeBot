package jukebot.commands;

import jukebot.DatabaseHandler;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Help implements Command {

    private final DatabaseHandler db = new DatabaseHandler();

    public void execute(GuildMessageReceivedEvent e, String query) {

        if (query.length() == 0) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Help Categories")
                    .setDescription("`1.` Getting Started\n`2.` Controls\n`3.` Media\n`4.` Miscellaneous\n\nUse `" + db.getPrefix(e.getGuild().getIdLong()) + "help <number>` to select a category")
                    .build()
            ).queue();
        } else {
            switch (query) {
                case "1":
                    e.getChannel().sendMessage(CreateHelpEmbed(
                                    "You can use the **play** command to make JukeBot join your channel, search for the specified song and begin playing.\n`"
                                            + db.getPrefix(e.getGuild().getIdLong()) + "play <URL/Search Query>`"
                    )).queue();
                    break;
                case "2":
                    e.getChannel().sendMessage(CreateHelpEmbed(
                            "[Additional help can be found on the website here](http://jukebot.xyz/documentation)\n\n"
                                    + "**`play       `**: Searches for the song and plays it immediately\n"
                                    + "**`select     `**: Searches YouTube and returns a list of tracks to choose from\n"
                                    + "**`scsearch   `**: Searches SoundCloud and queues the first result\n"
                                    + "**`togglepause`**: Toggles audio playback\n"
                                    + "**`stop       `**: Terminates the currently playing track and clears the queue\n"
                                    + "**`skip       `**: Initiates a vote to skip the currently playing track\n"
                                    + "**`forceskip  `**: Immediately skips the currently playing track without voting\n"
                                    + "**`repeat     `**: Allows toggling between queue, song or no looping\n"
                                    + "**`fastforward`**: Skip the specified amount of seconds into the current track\n"
                                    + "**`shuffle    `**: Toggle queue randomization\n"
                                    + "**`volume     `**: Change the volume of the bot's audio playback"
                    )).queue();
                    break;
                case "3":
                    e.getChannel().sendMessage(CreateHelpEmbed(
                            "[Additional help can be found on the website here](http://jukebot.xyz/documentation)\n\n"
                                    + "**`queue   `**: Displays the current queue\n"
                                    + "**`unqueue `**: Unqueue the track at the given position\n"
                                    + "**`now     `**: Displays information about the currently playing track\n"
                                    + "**`save    `**: Direct messages you information about the currently playing track\n"
                                    + "**`posthere`**: Uses the current channel for now playing messages\n"
                    )).queue();
                    break;
                case "4":
                    e.getChannel().sendMessage(CreateHelpEmbed(
                            "[Additional help can be found on the website here](http://jukebot.xyz/documentation)\n\n"
                                    + "**`prefix  `**: Allows changing of the current server prefix\n"
                                    + "**`help    `**: Displays the bot's help embed\n"
                                    + "**`invite  `**: Displays the bot's invite URL\n"
                                    + "**`patreon `**: Provides a direct link to the bot's patreon page\n"
                                    + "**`debug   `**: Displays bot statistics\n"
                                    + "**`donators`**: Developer command for managing donators statuses"
                    )).queue();
                    break;
                default:
                    e.getChannel().sendMessage(new EmbedBuilder()
                            .setColor(Bot.EmbedColour)
                            .setTitle("Invalid Category Specified")
                            .setDescription("`1.` Getting Started\n`2.` Controls\n`3.` Media\n`4.` Miscellaneous\n\nUse `" + db.getPrefix(e.getGuild().getIdLong()) + "help <number>` to select a category")
                            .build()
                    ).queue();
            }
        }

    }

    private MessageEmbed CreateHelpEmbed(String description) {
        return new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .setDescription(description)
                .build();
    }

}