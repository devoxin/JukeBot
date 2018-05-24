package jukebot.commands;

import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;
import net.dv8tion.jda.core.entities.MessageEmbed;

@CommandProperties(description = "Provides a link to JukeBot's Patreon", category = CommandProperties.category.MISC)
public class Patreon implements Command {

    public void execute(final Context context) {

        final MessageEmbed.Field[] fields = {
                new MessageEmbed.Field("Tier 1 ($1)", "-> Donator Role in [JukeBot's Server](https://discord.gg/xvtH2Yn)\n" +
                        "-> Ability to queue songs up to 5 hours long\n" +
                        "-> Ability to queue up to 1000 songs from a playlist\n" +
                        "-> Ability to queue livestreams", false),
                new MessageEmbed.Field("Tier 2 ($2)", "-> Donator Role in [JukeBot's Server](https://discord.gg/xvtH2Yn)\n" +
                        "-> Removed song duration limit\n" +
                        "-> Removed playlist importing limit", false)
        };

        context.sendEmbed("Become a Patron!", "[Click here to go to my Patreon page](https://patreon.com/Devoxin)", fields);
    }
}
