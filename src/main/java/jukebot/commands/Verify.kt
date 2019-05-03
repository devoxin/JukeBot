package jukebot.commands

import jukebot.Database
import jukebot.JukeBot
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.Helpers

@CommandProperties(description = "Receive your donor rewards if you're a patron")
class Verify : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
        context.channel.sendTyping().queue()

        JukeBot.patreonApi.fetchPledgesOfCampaign("750822").thenAccept { users ->
            if (users.isEmpty()) {
                return@thenAccept context.embed("Donor Verification", "Unable to retrieve a list of donors!\nWe're sorry for the inconvenience.")
            }

            val pledge = users.firstOrNull { it.discordId != null && it.discordId == context.author.idLong }
                    ?: return@thenAccept context.embed("Donor Verification",
                            "No Discord account linked to your Patreon account. Link your discord and try again.\n" +
                                    "If you continue to receive this error, please [join here](https://discord.gg/xvtH2Yn)")

            if (pledge.isDeclined) {
                return@thenAccept context.embed("Donor Verification", "It appears your payment has been declined. Please resolve this issue and then try again.\n" +
                        "If you believe this to be in error, please [join here](https://discord.gg/xvtH2Yn)")
            }

            val pledgeAmount = pledge.pledgeCents.toDouble() / 100
            val calculatedTier = Helpers.calculateTier(pledgeAmount)

            context.embed("Donor Verification", "Thanks for donating! **Your pledge: $${String.format("%1$,.2f", pledgeAmount)}**\n" +
                    "You qualify for **Tier $calculatedTier**!\n\n" +
                    "Your reward has been applied. If for some reason you encounter issues, please join https://discord.gg/xvtH2Yn")

            Database.setTier(context.author.idLong, calculatedTier)
        }
    }
}
