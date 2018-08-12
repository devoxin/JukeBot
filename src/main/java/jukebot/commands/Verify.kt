package jukebot.commands

import jukebot.Database
import jukebot.JukeBot
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.PatreonUser
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.future.await
import java.util.concurrent.CompletableFuture

@CommandProperties(description = "Receive your donor rewards if you're a patron")
class Verify : Command {

    override fun execute(context: Context) {
        async {
            context.channel.sendTyping().queue()

            val future = CompletableFuture<List<PatreonUser>>()
            JukeBot.patreonApi.fetchPledgesOfCampaign("750822", future)

            val users = future.await()

            if (users.isEmpty()) {
                return@async context.sendEmbed("Pledge Verification", "Unable to retrieve a list of donors!\nWe're sorry for the inconvenience.")
            }

            val pledge = users.firstOrNull { it.discordId != null && it.discordId == context.author.id }
                    ?: return@async context.sendEmbed("Pledge Verification",
                            "No Discord account linked to your Patreon account. Link your discord and try again.\n" +
                                    "If you continue to receive this error, please [join here](https://discord.gg/xvtH2Yn)")

            if (pledge.isDeclined) {
                return@async context.sendEmbed("Pledge Verification", "It appears your payment has been declined. Please resolve this issue and then try again.\n" +
                        "If you believe this to be in error, please [join here](https://discord.gg/xvtH2Yn)")
            }

            val pledgeAmount = pledge.pledgeCents.toDouble() / 100

            var tier = 0

            if (pledgeAmount >= 1 && pledgeAmount < 2) {
                tier = 1
            } else if (pledgeAmount >= 2) {
                tier = 2
            }

            context.sendEmbed("Pledge Verification", "Thanks for donating! **Your pledge: $${String.format("%1$,.2f", pledgeAmount)}**\n" +
                    "It looks like you qualify for **Tier $tier**!\n\nYour reward has been applied. If for some reason you encounter issues, please join https://discord.gg/xvtH2Yn")

            Database.setTier(context.author.idLong, tier)
        }
    }

}
