package jukebot.commands

import jukebot.Database
import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context
import jukebot.framework.SubCommand
import jukebot.utils.Helpers
import jukebot.utils.addFields
import jukebot.utils.decodeColor
import net.dv8tion.jda.api.entities.MessageEmbed
import java.text.DecimalFormat
import java.util.regex.Pattern

@CommandProperties(description = "Manage server-specific settings such as prefix etc", aliases = ["set", "config", "configure"])
class Settings : Command(ExecutionType.STANDARD) {

    private val mentionRegex = Pattern.compile("<@!?\\d{17,20}>")
    private val dpFormatter = DecimalFormat("0.00")

    override fun execute(context: Context) {
        val sc = context.args.firstOrNull()?.toLowerCase() ?: ""

        if (!this.subcommands.containsKey(sc)) {
            return context.embed(
                "Server Settings",
                this.subcommands.map { "**`${Helpers.pad(it.key)}:`** ${it.value.description}" }.sorted().joinToString("\n")
            )
        }

        this.subcommands[sc]!!.invoke(context, true)
    }

    @SubCommand(trigger = "prefix", description = "Sets the server prefix")
    fun prefix(ctx: Context, args: List<String>) {
        if (args.isEmpty()) {
            return ctx.embed("Invalid Prefix", "You need to specify a new server prefix.")
        }

        val newPrefix = args.joinToString(" ")

        if (mentionRegex.matcher(newPrefix).matches()) {
            return ctx.embed("Invalid Prefix", "Mentions cannot be used as prefixes.")
        }

        Database.setPrefix(ctx.guild.idLong, newPrefix)
        ctx.embed("Server Prefix Updated", "The new prefix for this server is `$newPrefix`")
    }

    @SubCommand(trigger = "djrole", description = "Sets the DJ role for the server")
    fun djrole(ctx: Context, args: List<String>) {
        if (args.isEmpty()) {
            return ctx.embed("Invalid Role", "You need to specify the name of the new role (case-sensitive).")
        }

        val roleName = args.joinToString(" ")

        if (roleName == "reset") {
            Database.setDjRole(ctx.guild.idLong, null)
            return ctx.embed("DJ Role Updated", "Reset DJ role to `Default (DJ)`")
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

    @SubCommand(trigger = "votes", description = "Sets the vote-skip percentage threshold")
    fun votes(ctx: Context, args: List<String>) {
        val threshold = args.firstOrNull()?.toDouble()
            ?: return ctx.embed("Invalid Threshold", "You need to specify a number between `0-100`")

        if (threshold < 0 || threshold > 100) {
            return ctx.embed("Invalid Threshold", "You need to specify a number between `0-100`")
        }

        val formattedThreshold = dpFormatter.format(threshold)
        Database.setSkipThreshold(ctx.guild.idLong, threshold / 100)
        ctx.embed("Skip Threshold Updated", "Skip Vote Threshold set to `$formattedThreshold%`")
    }

    @SubCommand(trigger = "embedcolor", description = "Sets the colour used for embeds")
    fun embedcolor(ctx: Context, args: List<String>) {
        val color = decodeColor(args.firstOrNull() ?: "")
            ?: return ctx.embed("Invalid Colour", "You need to specify a valid hex. Example: `#1E90FF`")

        Database.setColour(ctx.guild.idLong, color.rgb)
        ctx.embed {
            setColor(color.rgb)
            setTitle("Colour Updated")
            setDescription("Set new colour to `${args.first()}`")
        }
    }

    @SubCommand(trigger = "musicnick", description = "Sets whether the nickname displays the current track")
    fun musicnick(ctx: Context, args: List<String>) {
        val opt = when (args.firstOrNull()) {
            "on" -> true
            "off" -> false
            else -> return ctx.embed("Invalid Option", "You need to specify a valid option (`on`/`off`)")
        }

        Database.setMusicNickEnabled(ctx.guild.idLong, opt)

        val human = if (opt) "enabled" else "disabled"
        ctx.embed("Music Nick Updated", "Nickname changing for playing tracks `$human`")
    }

    @SubCommand(trigger = "autoplay", description = "Sets whether to autoplay once the queue is empty.")
    fun autoplay(ctx: Context, args: List<String>) {
        if (!Database.isPremiumServer(ctx.guild.idLong)) {
            return ctx.embed("Server Settings", "This setting is only available for premium servers. Check `${ctx.prefix}patreon` for more info.")
        }

        val opt = when (args.firstOrNull()) {
            "on" -> true
            "off" -> false
            else -> return ctx.embed("Invalid Option", "You need to specify a valid option (`on`/`off`)")
        }

        Database.setAutoPlayEnabled(ctx.guild.idLong, opt)

        val human = if (opt) "enabled" else "disabled"
        ctx.embed("AutoPlay Updated", "AutoPlay is now `$human`")
    }

    @SubCommand(trigger = "autodc", description = "Toggle whether the bot disconnects upon empty VC")
    fun autodc(ctx: Context, args: List<String>) {
        if (!Database.isPremiumServer(ctx.guild.idLong)) {
            return ctx.embed("Server Settings", "This setting is only available for premium servers. Check `${ctx.prefix}patreon` for more info.")
        }

        val opt = when (args.firstOrNull()) {
            "on" -> false
            "off" -> true
            else -> return ctx.embed("Invalid Option", "You need to specify a valid option (`on`/`off`)")
        }

        Database.setAutoDcDisabled(ctx.guild.idLong, opt)

        val human = if (opt) "disabled" else "enabled"
        ctx.embed("Auto-DC Updated", "Auto-DC is now `$human`")
    }

    @SubCommand(trigger = "view", description = "Displays all settings and their values.")
    fun view(ctx: Context, args: List<String>) {
        val customDjRole: Long? = Database.getDjRole(ctx.guild.idLong)
        val djRoleFormatted = if (customDjRole != null) "<@&$customDjRole>" else "Default (DJ)"
        val skipThreshold = dpFormatter.format(Database.getSkipThreshold(ctx.guild.idLong) * 100)
        val hex = Integer.toHexString(ctx.embedColor and 0xffffff)
        val musicNick = if (Database.getIsMusicNickEnabled(ctx.guild.idLong)) "Enabled" else "Disabled"
        val autoPlay = if (Database.isPremiumServer(ctx.guild.idLong) &&
            Database.getIsAutoPlayEnabled(ctx.guild.idLong)) "Enabled" else "Disabled"
        val autoDc = if (!Database.isPremiumServer(ctx.guild.idLong) ||
            !Database.getIsAutoDcDisabled(ctx.guild.idLong)) "Enabled" else "Disabled"

        val fields = arrayOf(
            MessageEmbed.Field("Server Prefix", "`${ctx.prefix}`", true),
            MessageEmbed.Field("DJ Role", djRoleFormatted, true),
            MessageEmbed.Field("Skip Vote Threshold", "$skipThreshold%", true),
            MessageEmbed.Field("Embed Colour", hex, true),
            MessageEmbed.Field("Music Nickname", musicNick, true),
            MessageEmbed.Field("AutoPlay", autoPlay, true),
            MessageEmbed.Field("Auto-DC", autoDc, true),
            MessageEmbed.Field("\u200b", "\u200b", true),
            MessageEmbed.Field("\u200b", "\u200b", true)
        )

        ctx.embed {
            setTitle("Server Settings | ${ctx.guild.name}")
            addFields(fields)
        }
    }
}