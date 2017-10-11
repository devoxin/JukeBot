package jukebot.commands;

import jukebot.utils.Bot;
import jukebot.utils.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Invite implements Command {

    public void execute(GuildMessageReceivedEvent e, String query) {

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .addField("Add me to your server!", "[Click Here](https://discordapp.com/oauth2/authorize?permissions=36793345&scope=bot&client_id=249303797371895820)", true )
                .addField("Get support!", "[Click Here](https://discord.gg/xvtH2Yn)", true)
                .build()
        ).queue();

    }
}
