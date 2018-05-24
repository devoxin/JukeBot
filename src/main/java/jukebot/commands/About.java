package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import jukebot.JukeBot;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import org.sqlite.SQLiteJDBCLoader;

@CommandProperties(aliases = {"info"}, description = "Displays some information about the bot")
public class About implements Command {

    @Override
    public void execute(final Context context) {
        final String dependencies = "**JDA**: " + JDAInfo.VERSION +
                "\n**Lavaplayer**: " + PlayerLibrary.VERSION +
                "\n**SQLite**: " + SQLiteJDBCLoader.getVersion();

        final User dev = context.getJda().getUserById(180093157554388993L);
        final MessageEmbed.Field[] fields = {
                new MessageEmbed.Field("Dependencies", dependencies, true),
                new MessageEmbed.Field("Links", "[GitHub](https://github.com/Devoxin/JukeBot)\n[Website](https://jukebot.xyz)", true)
        };

        context.sendEmbed("About JukeBot " + JukeBot.VERSION,
                "Developer: **" + dev.getName() + "#" + dev.getDiscriminator() + "**",
                fields);
    }
}
