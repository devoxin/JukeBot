package jukebot.commands;

import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;
import net.dv8tion.jda.core.entities.MessageEmbed;

@CommandProperties(description = "Displays the bot's invite URL", category = CommandProperties.category.MISC)
public class Invite implements Command {

    public void execute(final Context context) {

        final MessageEmbed.Field[] fields = {
                new MessageEmbed.Field("Add me to your server!", "[Invite](https://discordapp.com/oauth2/authorize?permissions=36793345&scope=bot&client_id=249303797371895820)", true),
                new MessageEmbed.Field("Support Server", "[Invite](https://discord.gg/xvtH2Yn)", true)
        };

        context.sendEmbed("", "", fields);

    }
}
