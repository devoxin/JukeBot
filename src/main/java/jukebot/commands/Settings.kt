package jukebot.commands

import jukebot.Database
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import net.dv8tion.jda.core.entities.MessageEmbed
import java.util.regex.Pattern

@CommandProperties(description = "Manage server-specific settings such as prefix etc", aliases = ["set", "config", "configure"])
class Settings : Command {

    private val mentionRegex = Pattern.compile("<@!?\\d{17,20}>")

    override fun execute(context: Context) {
        if (context.args[0].isBlank()) {
            val customDjRole: Long? = Database.getDjRole(context.guild.idLong)
            val djRoleFormatted: String = if (customDjRole != null) "<@&$customDjRole>" else "Default (DJ)"

            val fields: Array<MessageEmbed.Field> = arrayOf(
                    MessageEmbed.Field("Server Prefix (prefix)", "`${context.prefix}`", true),
                    MessageEmbed.Field("DJ Role (role)", djRoleFormatted, true),
                    MessageEmbed.Field("\u200B", "\u200B", true)
            )

            return context.sendEmbed(null,
                    "Configure these options with `${context.prefix}settings <setting> <value>`\n" +
                            "So for prefix, it'd be `${context.prefix}settings prefix <new prefix>`",
                    fields)
        }

        if (!context.isDJ(false)) {
            context.sendEmbed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.xyz/faq)")
            return
        }

        if (context.args[0] == "prefix") {
            if (context.getArg(1).isBlank()) {
                return context.sendEmbed("Invalid Prefix", "You need to specify a new server prefix.")
            }

            val newPrefix: String = context.args.drop(1).joinToString(" ")

            if (mentionRegex.matcher(newPrefix).matches()) {
                context.sendEmbed("Invalid Prefix", "Mentions cannot be used as prefixes.")
                return
            }

            val prefixUpdated: Boolean = Database.setPrefix(context.guild.idLong, newPrefix)

            return if (prefixUpdated) {
                context.sendEmbed("Server Prefix Updated", "The new prefix for this server is `$newPrefix`")
            } else {
                context.sendEmbed("An Error Occurred", "Unable to update the server prefix.")
            }
        } else if (context.args[0] == "role") {
            if (context.getArg(1).isBlank()) {
                return context.sendEmbed("Invalid Role", "You need to specify the name of the new role (case-sensitive).")
            }

            val roleName = context.args.drop(1).joinToString(" ")

            if (roleName == "reset") {
                Database.setDjRole(context.guild.idLong, null)
                return context.sendEmbed("DJ Role Updated", "Reset DJ role to `Default (DJ)`")
            }

            val newRole = if (roleName == "everyone") {
                context.guild.publicRole
            } else {
                context.guild.getRolesByName(roleName, false).firstOrNull()
            }

            if (newRole == null) {
                context.sendEmbed("Invalid Role", "No roles found matching `$roleName`\n\n" +
                        "You can use the `everyone` role by specifying `everyone` as the rolename.\nYou can reset the role by specifying `reset` as the rolename.")
            } else {
                Database.setDjRole(context.guild.idLong, newRole.idLong)
                context.sendEmbed("DJ Role Updated", "New role set to <@&${newRole.idLong}>")
            }
        } else {
            context.sendEmbed("Unrecognised Setting", "`${context.args[0]}` is not a recognised setting\n\nValid settings: `prefix`, `role`")
        }
    }

}