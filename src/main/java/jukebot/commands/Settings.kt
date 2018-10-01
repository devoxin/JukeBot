package jukebot.commands

import jukebot.Database
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.addFields
import net.dv8tion.jda.core.entities.MessageEmbed
import java.text.DecimalFormat
import java.util.regex.Pattern

@CommandProperties(description = "Manage server-specific settings such as prefix etc", aliases = ["set", "config", "configure"])
class Settings : Command {

    private val mentionRegex = Pattern.compile("<@!?\\d{17,20}>")
    private val dpFormatter = DecimalFormat("0.00")

    override fun execute(context: Context) {
        if (context.getArg(0).isBlank()) {
            val customDjRole: Long? = Database.getDjRole(context.guild.idLong)
            val djRoleFormatted: String = if (customDjRole != null) "<@&$customDjRole>" else "Default (DJ)"
            val skipThreshold: String = dpFormatter.format(Database.getSkipThreshold(context.guild.idLong) * 100)

            val fields: Array<MessageEmbed.Field> = arrayOf(
                    MessageEmbed.Field("Server Prefix (prefix)", "`${context.prefix}`", true),
                    MessageEmbed.Field("DJ Role (role)", djRoleFormatted, true),
                    MessageEmbed.Field("Skip Vote Threshold (vote)", "$skipThreshold%", true)
            )

            return context.embed {
                setTitle("Server Settings | ${context.guild.name}")
                setDescription("Configure: `${context.prefix}settings <setting> <value>`")
                addFields(fields)
            }
        }

        if (!context.isDJ(false)) {
            context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.xyz/faq)")
            return
        }

        if (context.args[0] == "prefix") {
            if (context.getArg(1).isBlank()) {
                return context.embed("Invalid Prefix", "You need to specify a new server prefix.")
            }

            val newPrefix: String = context.args.drop(1).joinToString(" ")

            if (mentionRegex.matcher(newPrefix).matches()) {
                context.embed("Invalid Prefix", "Mentions cannot be used as prefixes.")
                return
            }

            Database.setPrefix(context.guild.idLong, newPrefix)
            context.embed("Server Prefix Updated", "The new prefix for this server is `$newPrefix`")
        } else if (context.args[0] == "role") {
            if (context.getArg(1).isBlank()) {
                return context.embed("Invalid Role", "You need to specify the name of the new role (case-sensitive).")
            }

            val roleName = context.args.drop(1).joinToString(" ")

            if (roleName == "reset") {
                Database.setDjRole(context.guild.idLong, null)
                return context.embed("DJ Role Updated", "Reset DJ role to `Default (DJ)`")
            }

            val newRole = if (roleName == "everyone") {
                context.guild.publicRole
            } else {
                context.guild.getRolesByName(roleName, false).firstOrNull()
            }

            if (newRole == null) {
                context.embed("Invalid Role", "No roles found matching `$roleName`\n\n" +
                        "You can use the `everyone` role by specifying `everyone` as the rolename.\nYou can reset the role by specifying `reset` as the rolename.")
            } else {
                Database.setDjRole(context.guild.idLong, newRole.idLong)
                context.embed("DJ Role Updated", "New role set to <@&${newRole.idLong}>")
            }
        } else if (context.args[0] == "vote") {
            if (context.getArg(1).isBlank()) {
                return context.embed("Invalid Threshold", "You need to specify a number between `0-100`")
            }

            val threshold = context.getArg(1).toDoubleOrNull()
                    ?: return context.embed("Invalid Threshold", "You need to specify a number between `0-100`")

            if (threshold < 0 || threshold > 100) {
                return context.embed("Invalid Threshold", "You need to specify a number between `0-100`")
            }

            val formattedThreshold = dpFormatter.format(threshold)
            Database.setSkipThreshold(context.guild.idLong, threshold / 100)
            context.embed("Skip Threshold Updated", "Skip Vote Threshold set to `$formattedThreshold%`")
        } else {
            context.embed("Unrecognised Setting", "`${context.args[0]}` is not a recognised setting\n\nValid settings: `prefix`, `role`, `vote`")
        }
    }

}