package jukebot.commands.misc

import jukebot.Database
import jukebot.JukeBot
import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context
import jukebot.framework.SubCommand
import jukebot.utils.Helpers

@CommandProperties(description = "Receive your donor rewards if you're a patron", aliases = ["perks", "rewards"])
class Verify : Command(ExecutionType.STANDARD) {
    override fun execute(context: Context) {
        val sc = context.args.firstOrNull() ?: ""

        if (!this.subcommands.containsKey(sc)) {
            return context.embed(
                "Donation Management",
                this.subcommands.map { "**`${Helpers.pad(it.key)}:`** ${it.value.description}" }.joinToString("\n")
            )
        }

        this.subcommands[sc]!!.invoke(context)
    }

    @SubCommand(trigger = "link", description = "Links your Discord to Patreon for automatic rewards")
    fun link(ctx: Context) {
        ctx.channel.sendTyping().queue()

        JukeBot.patreonApi.fetchPledgesOfCampaign("750822").thenAccept { users ->
            if (users.isEmpty()) {
                return@thenAccept ctx.embed("Donor Verification", "Unable to retrieve a list of donors!\nWe're sorry for the inconvenience.")
            }

            val pledge = users.firstOrNull { it.discordId != null && it.discordId == ctx.author.idLong }
                ?: return@thenAccept ctx.embed("Donor Verification",
                    "No Discord account [linked to your Patreon account]($DISCORD_LINK_ARTICLE). " +
                        "Link your discord and try again.\nIf you continue to receive this error, please " +
                        "[join here](${JukeBot.HOME_SERVER})")

            if (pledge.isDeclined) {
                return@thenAccept ctx.embed("Donor Verification", "It appears your payment has been declined. Please resolve this issue and then try again.\n" +
                    "If you believe this to be in error, please [join here](${JukeBot.HOME_SERVER})")
            }

            val pledgeAmount = pledge.pledgeCents.toDouble() / 100
            val calculatedTier = Helpers.calculateTier(pledgeAmount)

            val note = if (calculatedTier == 3) {
                "Apply your reward at any time by running `\$perks addserver` in the server you would like to " +
                    "enable your perks on."
            } else {
                "Your perks have been automatically applied."
            }

            ctx.embed("Donor Perks", "Thanks for donating! **Your pledge: $${String.format("%1$,.2f", pledgeAmount)}** " +
                "(Tier $calculatedTier)\n\n" +
                "$note\nIf for some reason you encounter issues, please join ${JukeBot.HOME_SERVER}")

            Database.setTier(ctx.author.idLong, calculatedTier)
        }
    }

    @SubCommand(trigger = "addserver", description = "Registers the current server to receive perks")
    fun addServer(ctx: Context) {
        val serverQuota = calculateServerQuota(ctx.author.idLong)

        if (serverQuota == 0) {
            return ctx.embed(
                "Perks | Server Management",
                "Your donation does not meet the minimum amount required to access this reward."
            )
        }

        val allServers = Database.getPremiumServersOf(ctx.author.idLong)
        val remainingServers = serverQuota - allServers.size

        if (remainingServers == 0) {
            return ctx.embed(
                "Perks | Server Management",
                "You cannot add more servers as you've reached your quota (**$serverQuota**)."
            )
        }

        if (Database.getIsPremiumServer(ctx.guild.idLong)) {
            return ctx.embed(
                "Perks | Server Management",
                "This server is already registered as a premium server."
            )
        }

        Database.setPremiumServer(ctx.author.idLong, ctx.guild.idLong)

        ctx.embed(
            "Perks | Server Management",
            "This server has been registered as part of your perks!\n" +
                "All members will now have access to tier 2 rewards.\n\n" +
                "You may unregister this server after **28 days** from now."
        )
    }

    @SubCommand(trigger = "removeserver", description = "Unregisters the given server and revokes its perks")
    fun removeServer(ctx: Context) {
        val scArgs = ctx.args.drop(1)
        val sm = JukeBot.shardManager
        val allServers = Database.getPremiumServersOf(ctx.author.idLong)

        if (scArgs.isEmpty()) {
            val sb = StringBuilder()

            sb.append("Server ID            | Server Name    | Registered  |\n")
            sb.append("---------------------+----------------+-------------+\n")

            for (ps in allServers) {
                val guildName = sm.getGuildById(ps.guildId)?.name ?: "Unknown Server"
                val days = ps.daysSinceAdded()
                val plural = if (days == 1L) "" else "s"

                sb.append(String.format("%-21s ", ps.guildId))
                sb.append(String.format("%-16.16s ", guildName))
                sb.append(days).append(" day").append(plural).append(" ago\n")
            }

            return ctx.channel.sendMessage(
                "**Perks | Server Management**\n" +
                    "You need to specify the ID of the server you want to unregister.\n\n" +
                    "```\n$sb```"
            ).queue()
        }

        val guildId = scArgs[0].toLongOrNull()

        if (guildId == null || !allServers.any { it.guildId == guildId }) {
            return ctx.embed(
                "Perks | Server Management",
                "Invalid server ID. Run this command without arguments to view a list of registered servers."
            )
        }

        val selectedGuild = allServers.first { it.guildId == guildId }

        if (selectedGuild.daysSinceAdded() < 28) {
            return ctx.embed(
                "Perks | Server Management",
                "This server was registered less than 28 days ago. It cannot be unregistered " +
                    "until at least 28 days have elapsed since registration to prevent abuse.\n\n" +
                    "If you have a valid reason for early de-registration, join ${JukeBot.HOME_SERVER}"
            )
        }

        Database.removePremiumServer(guildId)
        ctx.embed("Perks | Server Management", "Server unregistered successfully.")
    }

    fun calculateServerQuota(userId: Long): Int {
        val pledge = if (userId == JukeBot.botOwnerId) Integer.MAX_VALUE else Database.getTier(userId)

        if (pledge < 3) {
            return 0
        }

        return ((pledge - 3) / 1) + 1
        // Going back through this code, I'm really wondering why I divided by 1,
        // but I dare not change it because knowing my luck something would break
    }

    companion object {
        private const val DISCORD_LINK_ARTICLE = "https://support.patreon.com/hc/en-gb/articles/212052266-How-do-I-connect-Discord-to-Patreon-Patron-"
    }
}
