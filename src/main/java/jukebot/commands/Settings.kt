package jukebot.commands

import jukebot.Database
import jukebot.utils.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.MessageEmbed
import java.text.DecimalFormat
import java.util.regex.Pattern

@CommandProperties(description = "Manage server-specific settings such as prefix etc", aliases = ["set", "config", "configure"])
class Settings : Command(ExecutionType.STANDARD) {

    private val mentionRegex = Pattern.compile("<@!?\\d{17,20}>")
    private val dpFormatter = DecimalFormat("0.00")

    private val availableSettings = hashMapOf<String, Setting>()
    private val validFields = arrayOf("prefix", "role", "vote", "colour", "nick").joinToString("`, `", prefix = "`", postfix = "`")

    init {
        availableSettings["prefix"] = Setting("Sets the server prefix") { ctx, firstArg ->
            if (firstArg == null) {
                return@Setting ctx.embed("Invalid Prefix", "You need to specify a new server prefix.")
            }

            val newPrefix = ctx.args.drop(1).joinToString(" ")

            if (mentionRegex.matcher(newPrefix).matches()) {
                return@Setting ctx.embed("Invalid Prefix", "Mentions cannot be used as prefixes.")
            }

            Database.setPrefix(ctx.guild.idLong, newPrefix)
            ctx.embed("Server Prefix Updated", "The new prefix for this server is `$newPrefix`")
        }

        availableSettings["djrole"] = Setting("Sets the DJ role for the server") { ctx, firstArg ->
            if (firstArg == null) {
                return@Setting ctx.embed("Invalid Role", "You need to specify the name of the new role (case-sensitive).")
            }

            val roleName = ctx.args.drop(1).joinToString(" ")

            if (roleName == "reset") {
                Database.setDjRole(ctx.guild.idLong, null)
                return@Setting ctx.embed("DJ Role Updated", "Reset DJ role to `Default (DJ)`")
            }

            val newRole = if (roleName == "everyone") {
                ctx.guild.publicRole
            } else {
                ctx.guild.getRolesByName(roleName, false).firstOrNull()
            }

            if (newRole == null) {
                ctx.embed("Invalid Role", "No roles found matching `$roleName`\n\n" +
                        "You can use the `everyone` role by specifying `everyone` as the rolename.\nYou can reset the role by specifying `reset` as the rolename.")
            } else {
                Database.setDjRole(ctx.guild.idLong, newRole.idLong)
                ctx.embed("DJ Role Updated", "New role set to <@&${newRole.idLong}>")
            }
        }

        availableSettings["votes"] = Setting("Sets the vote-skip percentage threshold") { ctx, pc ->
            val threshold = pc?.toDouble() ?: return@Setting ctx.embed("Invalid Threshold", "You need to specify a number between `0-100`")

            if (threshold < 0 || threshold > 100) {
                return@Setting ctx.embed("Invalid Threshold", "You need to specify a number between `0-100`")
            }

            val formattedThreshold = dpFormatter.format(threshold)
            Database.setSkipThreshold(ctx.guild.idLong, threshold / 100)
            ctx.embed("Skip Threshold Updated", "Skip Vote Threshold set to `$formattedThreshold%`")
        }

        availableSettings["embedcolor"] = Setting("Sets the colour used for embeds") { ctx, colour ->
            val color = decodeColor(colour ?: "")
                    ?: return@Setting ctx.embed("Invalid Colour", "You need to specify a valid hex. Example: `#1E90FF`")

            Database.setColour(ctx.guild.idLong, color.rgb)
            ctx.embed {
                setColor(color.rgb)
                setTitle("Colour Updated")
                setDescription("Set new colour to `$colour`")
            }
        }

        availableSettings["musicnick"] = Setting("Sets whether the nickname displays the current track") { ctx, setting ->
            val opt = when (setting ?: "") {
                "on" -> true
                "off" -> false
                else -> return@Setting ctx.embed("Invalid Option", "You need to specify a valid option (`on`/`off`)")
            }

            Database.setMusicNickEnabled(ctx.guild.idLong, opt)

            val human = if (opt) "enabled" else "disabled"
            ctx.embed("Music Nick Updated", "Nickname changing for playing tracks `$human`")
        }

        availableSettings["view"] = Setting("Displays all settings and their values.") { ctx, _ ->
            val customDjRole: Long? = Database.getDjRole(ctx.guild.idLong)
            val djRoleFormatted = if (customDjRole != null) "<@&$customDjRole>" else "Default (DJ)"
            val skipThreshold = dpFormatter.format(Database.getSkipThreshold(ctx.guild.idLong) * 100)
            val hex = Integer.toHexString(ctx.embedColor and 0xffffff)
            val musicNick = if (Database.getIsMusicNickEnabled(ctx.guild.idLong)) "Enabled" else "Disabled"

            val fields = arrayOf(
                    MessageEmbed.Field("Server Prefix (prefix)", "`${ctx.prefix}`", true),
                    MessageEmbed.Field("DJ Role (role)", djRoleFormatted, true),
                    MessageEmbed.Field("Skip Vote Threshold (vote)", "$skipThreshold%", true),
                    MessageEmbed.Field("Embed Colour (colour)", hex, true),
                    MessageEmbed.Field("Music Nickname (nick)", musicNick, true),
                    MessageEmbed.Field("\u200b", "\u200b", true)
            )

            ctx.embed {
                setTitle("Server Settings | ${ctx.guild.name}")
                addFields(fields)
            }
        }
    }

    override fun execute(context: Context) {
        val option = context.args.firstOrNull()?.toLowerCase()

        if (option == null || !availableSettings.containsKey(option)) {
            val builder = StringBuilder()

            for ((key, extra) in availableSettings.toSortedMap()) {
                builder.append("**`")
                        .append(String.format("%-11s", key.toLowerCase()).replace(" ", " \u200B"))
                        .append(":`** ")
                        .append(extra.description)
                        .append("\n")
            }

            context.embed {
                setTitle("Available Server Settings")
                setDescription(builder)
                setFooter("You can use ${context.prefix}settings <category> to modify server settings.", null)
            }

            return
        }

        if (!context.isDJ(false)) {
            return context.embed(
                    "Not a DJ",
                    "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.serux.pro/faq)"
            )
        }

        val firstArg = context.args.getOrNull(1)
        availableSettings[option]!!.func(context, firstArg)
    }

    class Setting(val description: String, val func: (Context, String?) -> Unit)

}