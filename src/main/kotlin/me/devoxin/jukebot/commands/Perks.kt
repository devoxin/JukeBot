package me.devoxin.jukebot.commands

import kotlinx.coroutines.future.await
import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.entities.Cog
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.Launcher
import me.devoxin.jukebot.annotations.Checks.Premium
import me.devoxin.jukebot.extensions.embed
import me.devoxin.jukebot.extensions.premiumUser
import me.devoxin.jukebot.extensions.respondUnit
import me.devoxin.jukebot.integrations.patreon.PatreonTier
import me.devoxin.jukebot.models.PremiumUser
import me.devoxin.jukebot.utils.Constants

class Perks : Cog {
    override fun name() = "Misc"

    @Command(description = "Manage your premium perks.")
    fun perks(ctx: Context) {
        val cmd = ctx.invokedCommand as? CommandFunction
            ?: return

        val padLength = cmd.subcommands.keys.maxOf { it.length }

        val subcommands = cmd.subcommands.values.sortedBy { it.name }.joinToString("\n") {
            "`${it.name.padEnd(padLength)}` — ${it.properties.description}"
        }

        ctx.embed("Subcommand Required", subcommands)
    }

    @SubCommand(description = "Links your Discord to Patreon for automatic rewards")
    suspend fun link(ctx: Context) {
        if (Launcher.patreonApi == null) {
            return ctx.send("Command unavailable.")
        }

        ctx.think().await()

        val pledges = Launcher.patreonApi!!.fetchPledges("750822").await()

        if (pledges.isEmpty()) {
            return ctx.embed(
                "Perks",
                "Unable to retrieve current Patrons.\nWe're sorry for any inconvenience, please try again later.\nIf the issue persists, [join the support server](<${Constants.HOME_SERVER}>)"
            )
        }

        val pledge = pledges.firstOrNull { it.discordUserId != null && it.discordUserId == ctx.author.idLong }
            ?: return ctx.embed(
                "Perks",
                "Couldn't find your account.\n[Re-link your account]($DISCORD_LINK_ARTICLE) and try again.\n" +
                    "If you continue to receive this error, please [join the support server](${Constants.HOME_SERVER})"
            )

        if (pledge.isDeclined || pledge.entitledAmountCents <= 0) {
            return ctx.embed(
                "Perks",
                "It appears your payment has been declined, or is too low to qualify for any perk tiers.\n" +
                    "Please resolve this issue and then try again.\n" +
                    "If you believe this to be in error, please [join the support server](${Constants.HOME_SERVER})"
            )
        }

        if (pledge.highestTier == PatreonTier.UNKNOWN) {
            return ctx.embed("Perks", "An unknown error occurred.\n" +
                "Your tier (${pledge.highestTier.tierName}) doesn't appear to be valid.\n" +
                "Please [join the support server](${Constants.HOME_SERVER})")
        }

        val user = PremiumUser(ctx.author.idLong, pledge.highestTier.tierId, pledge.entitledAmountCents)
        user.save()

        ctx.embed(
            "Perks",
            "Thanks for subscribing!\n" +
                "Your tier: **${pledge.highestTier.tierName}**\n" +
                "Your perks have been enabled and are ready to use.\nYou can [join the support server](${Constants.HOME_SERVER}) for any issues, and for your Patron role."
        )
    }

    @SubCommand(description = "Registers the current server to receive perks", guildOnly = true)
    @Premium(allowShared = false)
    fun addServer(ctx: Context) {
        val premiumUser = ctx.premiumUser?.takeIf { !it.shared }
            ?: return ctx.embed("Perks (Server Management)", "Sorry, you need to be [subscribed to Premium](https://patreon.com/devoxin) to use this.")

        val entitledServerCount = premiumUser.tier.entitledPremiumServers

        if (entitledServerCount == 0) {
            return ctx.embed("Perks (Server Management)", "Your tier doesn't entitle you to any premium servers.")
        }

        val remainingServerSlots = entitledServerCount - premiumUser.guilds.size

        if (remainingServerSlots <= 0) {
            return ctx.embed(
                "Perks (Server Management)",
                "You don't have any server slots remaining."
            )
        }

        if (Database.getIsPremiumServer(ctx.guild!!.idLong)) {
            return ctx.embed(
                "Perks (Server Management)",
                "This server is already registered as a premium server."
            )
        }

        Database.setPremiumServer(ctx.author.idLong, ctx.guild!!.idLong)

        ctx.embed {
            setTitle("Perks (Server Management)")
            setDescription("This server is now marked as premium.\n" +
                "All members will benefit from **Personal** perks.")
            setFooter("Exclusions apply.")
        }
    }

    @SubCommand(description = "Unregisters the given server and revokes its perks", guildOnly = true)
    fun removeServer(ctx: Context, serverId: String?) {
        val sm = Launcher.shardManager
        val allServers = Database.getPremiumServersOf(ctx.author.idLong)

        if (serverId == null) {
            val sb = buildString {
                append("Server ID            | Server Name    | Registered  |\n")
                append("---------------------+----------------+-------------+\n")

                for (ps in allServers) {
                    val guildName = sm.getGuildById(ps.guildId)?.name ?: "Unknown Server"
                    val days = ps.daysSinceAdded
                    val plural = if (days == 1L) "" else "s"

                    append(String.format("%-21s ", ps.guildId))
                    append(String.format("%-16.16s ", guildName))
                    append(days).append(" day").append(plural).append(" ago\n")
                }
            }

            return ctx.respondUnit("**Perks (Server Management)**\n" +
                "You need to specify the ID of the server you want to unregister.\n\n" +
                "```\n$sb```"
            )
        }

        val sidLong = serverId.toLongOrNull()
            ?: return ctx.respondUnit("You need to provide the ID of the server you wish to remove.")

        val selectedServer = allServers.firstOrNull { it.guildId == sidLong }
            ?: return ctx.embed(
                "Perks (Server Management)",
                "Invalid server ID. Run this command without arguments to view a list of registered servers."
            )

        if (selectedServer.daysSinceAdded < 28 && ctx.author.idLong !in ctx.commandClient.ownerIds) {
            return ctx.embed(
                "Perks (Server Management)",
                "This server was registered less than 28 days ago. It cannot be unregistered " +
                    "until at least 28 days have elapsed since registration to prevent abuse.\n\n" +
                    "If you believe this to be in error, please [join the support server](${Constants.HOME_SERVER})"
            )
        }

        Database.removePremiumServer(sidLong)
        ctx.embed("Perks (Server Management)", "Server unregistered successfully.")
    }

    companion object {
        private const val DISCORD_LINK_ARTICLE =
            "https://support.patreon.com/hc/en-gb/articles/212052266-How-do-I-connect-Discord-to-Patreon-Patron-"
    }
}
