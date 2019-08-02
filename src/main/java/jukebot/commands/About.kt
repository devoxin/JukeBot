package jukebot.commands

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary
import jukebot.JukeBot
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.addFields
import net.dv8tion.jda.api.JDAInfo
import net.dv8tion.jda.api.entities.MessageEmbed
import org.sqlite.SQLiteJDBCLoader

@CommandProperties(aliases = ["info"], description = "Displays some information about the bot")
class About : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        val dependencies = "**JDA**: ${JDAInfo.VERSION}" +
                "\n**Lavaplayer**: ${PlayerLibrary.VERSION}" +
                "\n**SQLite**: ${SQLiteJDBCLoader.getVersion()}"

        val fields = arrayOf(
                MessageEmbed.Field("Dependencies", dependencies, true),
                MessageEmbed.Field("Links", "[GitHub](https://github.com/Devoxin/JukeBot)\n[Website](https://jukebot.serux.pro)", true)
        )

        context.embed {
            setTitle("JukeBot ${JukeBot.VERSION}")
            setDescription("Developer: **Devoxin#0101**")
            addFields(fields)
        }
    }
}
