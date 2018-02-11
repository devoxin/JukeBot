package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import jukebot.JukeBot;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.sqlite.SQLiteJDBCLoader;

@CommandProperties(aliases = {"info"}, description = "Displays some information about the bot")
public class About implements Command {

    @Override
    public void execute(GuildMessageReceivedEvent e, String query) {
        final String dependencies = "**JDA**: " + JDAInfo.VERSION +
                "\n**Lavaplayer**: " + PlayerLibrary.VERSION +
                "\n**SQLite**: " + SQLiteJDBCLoader.getVersion();

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(JukeBot.embedColour)
                .setTitle("About JukeBot " + JukeBot.VERSION + "!")
                .setDescription("Developer: **Kromatic#0420**")
                .addField("Dependencies", dependencies, true)
                .addField("Links", "[GitHub](https://github.com/Devoxin/JukeBot)\n[Website](http://jukebot.xyz)", true)
                .build()
        ).queue();
    }
}
