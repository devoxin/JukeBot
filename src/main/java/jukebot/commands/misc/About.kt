package jukebot.commands.misc

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary
import jukebot.JukeBot
import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context
import jukebot.utils.addFields
import net.dv8tion.jda.api.JDAInfo
import net.dv8tion.jda.api.entities.MessageEmbed
import org.sqlite.SQLiteJDBCLoader

@CommandProperties(aliases = ["info"], description = "Displays some information about the bot")
class About : Command(ExecutionType.STANDARD) {
    private val dependencies = mapOf(
        "JDA" to JDAInfo.VERSION,
        "Lavaplayer" to PlayerLibrary.VERSION,
        "SQLite" to SQLiteJDBCLoader.getVersion()
    )

    private val links = mapOf(
        "GitHub" to "https://github.com/Devoxin/JukeBot",
        "Website" to "https://jukebot.serux.pro"
    )

    override fun execute(context: Context) {
        val fields = arrayOf(
            *dependencies.map { MessageEmbed.Field(it.key, it.value, true) }.toTypedArray(),
            MessageEmbed.Field("Links", links.map { "[${it.key}](${it.value})"}.joinToString(" | "), true)
        )

        context.embed {
            setTitle("JukeBot ${JukeBot.VERSION}")
            setDescription("Developed by **devoxin#0101**")
            addFields(fields)
        }
    }
}
