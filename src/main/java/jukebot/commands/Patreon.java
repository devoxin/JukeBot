package jukebot.commands;

import jukebot.utils.Bot;
import jukebot.utils.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.awt.Color;

public class Patreon implements Command {

    public void execute(MessageReceivedEvent e, String query) {

        e.getTextChannel().sendMessage(new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .setTitle("Become a Patron!", "https://patreon.com/Devoxin")
                .setDescription("By becoming a patron, you'll have access to extra features within JukeBot!")
                .addField("Tier 1 ($2)", "-> Donator Role in [JukeBot's Server](https://discord.gg/xvtH2Yn)\n" +
                        "-> Ability to queue songs up to 5 hours long\n" +
                        "-> Ability to queue up to 1000 songs from a playlist\n" +
                        "-> Ability to queue livestreams", false)
                .addField("Tier 2 ($5)", "-> All the benefits of Tier 1 plus...\n" +
                        "-> Access to the volume command\n" +
                        "-> Access to the fastforward command\n" +
                        "-> Get JukeBot Patron added to a server of your choice!", false)
                .addField("Tier 3 ($10)", "-> All the benefits of Tier 1 and Tier 2 plus...\n" +
                        "-> Get JukeBot Patron added to two servers of your choice!\n" +
                        "-> Give a friend of your choice access to Tier 2 benefits!\n", false)
                .build()
        ).queue();

    }
}
