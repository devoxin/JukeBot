package me.devoxin.jukebot.commands.misc

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary
import me.devoxin.jukebot.framework.Command
import me.devoxin.jukebot.framework.CommandProperties
import me.devoxin.jukebot.framework.Context
import me.devoxin.jukebot.utils.Constants
import me.devoxin.jukebot.utils.Helpers
import me.devoxin.jukebot.utils.addFields
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
        "Website" to Constants.WEBSITE
    )

    override fun execute(context: Context) {
        val fields = arrayOf(
            *dependencies.map { MessageEmbed.Field(it.key, it.value, true) }.toTypedArray(),
            MessageEmbed.Field("Links", links.map { "[${it.key}](${it.value})" }.joinToString(" | "), true)
        )

        val commitHash = Helpers.version
        val commitUrl = "https://github.com/devoxin/JukeBot/commit/$commitHash"

        context.embed {
            setTitle("JukeBot (Revision $commitHash)", commitUrl)
            setDescription("Developed by **devoxin#4243**")
            addFields(fields)
        }
    }
}
