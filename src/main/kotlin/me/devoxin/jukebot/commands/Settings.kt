package me.devoxin.jukebot.commands

import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Describe
import me.devoxin.flight.api.annotations.Range
import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.entities.Cog
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.Launcher
import me.devoxin.jukebot.annotations.Checks.DJ
import me.devoxin.jukebot.extensions.*
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.requests.GatewayIntent

class Settings : Cog {
    override fun name() = "Configuration"

    @Command(aliases = ["config", "configure", "set"], description = "Manage bot settings for this server.", guildOnly = true)
    @DJ(alone = false)
    fun settings(ctx: Context) {
        val cmd = ctx.invokedCommand as? CommandFunction
            ?: return

        val padLength = cmd.subcommands.keys.maxOf { it.length }

        val subcommands = cmd.subcommands.values.joinToString("\n") {
            "`${it.name.padEnd(padLength)}` — ${it.properties.description}"
        }

        ctx.embed("Subcommand Required", subcommands)
    }

    @SubCommand(description = "Set the server prefix.")
    fun prefix(ctx: Context,
               @Describe("The new prefix for the server. Only affects message commands.")
               newPrefix: String) {
        if (GatewayIntent.MESSAGE_CONTENT !in Launcher.shardManager.gatewayIntents) {
            return ctx.embed("Setting Unavailable", "This setting isn't available for this JukeBot instance.\nReason: Launched without message content access.\n\nYou'll need to use the @mention prefix or slash commands instead.")
        }

        if (newPrefix matches mentionRegex) {
            return ctx.embed("Settings (Prefix)", "You can't use a mention as the server's main prefix.")
        }

        Database.setPrefix(ctx.guild!!.idLong, newPrefix)
        ctx.embed("Settings (Prefix)", "Prefix updated.\nFrom now on, you'll need to invoke commands with `$newPrefix`.")
    }

    @SubCommand(description = "Set the DJ role for the server.")
    fun djrole(ctx: Context, role: Role?) {
        if (role == null) {
            Database.setDjRole(ctx.guild!!.idLong, null)
            return ctx.embed("Settings (DJ Role)", "The DJ role has been reset to default (`DJ`).")
        }

        Database.setDjRole(ctx.guild!!.idLong, role.idLong)
        ctx.embed("Settings (DJ Role)", "The DJ role has been set to ${role.asMention}")
    }

    @SubCommand(description = "Sets the vote-skip percentage threshold.")
    fun votes(ctx: Context,
              @Describe("The percentage of votes required to pass (default is 50).")
              @Range(long = [0, 100])
              threshold: Int) {
        Database.setSkipThreshold(ctx.guild!!.idLong, (threshold / 100).toDouble())
        ctx.embed("Settings (Votes)", "The skip vote threshold has been set to `$threshold`%.")
    }


    @SubCommand(aliases = ["embedcolour", "colour", "color"], description = "Sets the colour used for embeds.")
    fun embedcolor(ctx: Context,
                   @Describe("The new colour. This can be a hex code, or RGB.")
                   colour: String) {
        val parts = colour.split(' ')

        val color = when (parts.size) {
            1 -> parts.first()
            3 -> {
                val (r, g, b) = parts.map { it.toIntOrNull()?.coerceIn(0, 255) }

                if (r == null || g == null || b == null) {
                    return ctx.embed("Settings (Embed Color)", "RGB must be 3 space-separated numbers between 0-255")
                }

                String.format("#%02x%02x%02x", r, g, b)
            }
            else -> null
        }?.toColorOrNull() ?: return ctx.embed(
            "Settings (Embed Color)",
            "You must specify either a [hex code or RGB](https://www.w3schools.com/colors/colors_picker.asp)"
        )

        Database.setColour(ctx.guild!!.idLong, color.rgb)

        ctx.embed {
            setColor(color.rgb)
            setTitle("Colour Updated")
            setDescription("Set new colour to `${String.format("#%02x%02x%02x", color.red, color.green, color.blue)}`")
        }
    }

    @SubCommand(aliases = ["nickname", "nick"], description = "Sets whether the nickname displays the current track.")
    fun musicnick(ctx: Context, enabled: Boolean) {
        if (enabled && ctx.premiumUser == null) {
            return ctx.embed("Premium Required", "Sorry, you can't enable this without a [Premium subscription](https://patreon.com/devoxin)")
        }

        Database.setMusicNickEnabled(ctx.guild!!.idLong, enabled)
        ctx.embed("Music Nick Updated", "Nickname changing for playing tracks `${enabled.humanized()}`")
    }

    @SubCommand(description = "Set whether the bot finds songs to play when queue is empty.")
    fun autoplay(ctx: Context, enabled: Boolean) {
        if (enabled && ctx.premiumUser == null) {
            return ctx.embed("Premium Required", "Sorry, you can't enable this without a [Premium subscription](https://patreon.com/devoxin)")
        }

        Database.setAutoPlayEnabled(ctx.guild!!.idLong, enabled)
        ctx.embed("AutoPlay Updated", "AutoPlay is now `${enabled.humanized()}`")
    }

    @SubCommand(description = "Set whether the bot disconnects when alone in a voice channel.")
    fun autodc(ctx: Context, enabled: Boolean) {
        if (enabled && ctx.premiumUser == null) {
            return ctx.embed("Premium Required", "Sorry, you can't enable this without a [Premium subscription](https://patreon.com/devoxin)")
        }

        Database.setAutoDcDisabled(ctx.guild!!.idLong, enabled)
        ctx.embed("Auto-DC Updated", "Auto-DC is now `${enabled.humanized()}`")
    }

    @SubCommand(description = "Displays the current server settings.")
    fun view(ctx: Context) {
        val customDjRole = Database.getDjRole(ctx.guild!!.idLong)
        val musicNick = (Database.getIsPremiumServer(ctx.guild!!.idLong) && Database.getIsMusicNickEnabled(ctx.guild!!.idLong)).humanized().capitalise()
        val autoPlay = (Database.getIsPremiumServer(ctx.guild!!.idLong) && Database.getIsAutoPlayEnabled(ctx.guild!!.idLong)).humanized().capitalise()
        val autoDc = (!Database.getIsPremiumServer(ctx.guild!!.idLong) || !Database.getIsAutoDcDisabled(ctx.guild!!.idLong)).humanized().capitalise()

        ctx.embed {
            setTitle("Server Settings for ${ctx.guild!!.name}")
            addField("Server Prefix", Database.getPrefix(ctx.guild!!.idLong), true)
            addField("DJ Role", customDjRole?.let { "<@&$it>" } ?: "Default (DJ)", true)
            addField("Skip Vote %", "${(Database.getSkipThreshold(ctx.guild!!.idLong) * 100).toInt()}%", true)
            addField("Embed Color", "#${Integer.toHexString(ctx.embedColor and 0xffffff)}", true)
            addField("Music Nickname", musicNick, true)
            addField("AutoPlay", autoPlay, true)
            addField("Auto-DC", autoDc, true)
            addBlankField(true)
            addBlankField(true)
        }
    }

    companion object {
        private val mentionRegex = "<@!?\\d{17,21}>".toRegex()
    }
}
