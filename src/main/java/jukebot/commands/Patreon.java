package jukebot.commands;

import jukebot.JukeBot;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(description = "Provides a link to JukeBot's Patreon", category = CommandProperties.category.MISC)
public class Patreon implements Command {

    public void execute(GuildMessageReceivedEvent e, String query) {

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(JukeBot.embedColour)
                .setTitle("Become a Patron!", "https://patreon.com/Devoxin")
                .setDescription("By becoming a patron, you'll have access to extra features within JukeBot!")
                .addField("Tier 1 ($1)", "-> Donator Role in [JukeBot's Server](https://discord.gg/xvtH2Yn)\n" +
                        "-> Ability to queue songs up to 5 hours long\n" +
                        "-> Ability to queue up to 1000 songs from a playlist\n" +
                        "-> Ability to queue livestreams", false)
                .addField("Tier 2 ($2)", "-> Donator Role in [JukeBot's Server](https://discord.gg/xvtH2Yn)\n" +
                        "-> Removed song duration limit\n" +
                        "-> Removed playlist importing limit", false)
                .addField("Tier 3 ($3)", "-> All the benefits of Tier 2 plus...\n" +
                        "-> Coming soon", false)
                .build()
        ).queue();

    }
}
