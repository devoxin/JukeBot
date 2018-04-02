package jukebot.commands;

import jukebot.JukeBot;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(description = "Displays the bot's invite URL", category = CommandProperties.category.MISC)
public class Invite implements Command {

    public void execute(GuildMessageReceivedEvent e, String query) {

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(JukeBot.embedColour)
                .addField("Add me to your server!", "[Click Here](https://discordapp.com/oauth2/authorize?permissions=36793345&scope=bot&client_id=249303797371895820)", true)
                .addField("Get support!", "[Click Here](https://discord.gg/xvtH2Yn)", true)
                .build()
        ).queue();

    }
}
