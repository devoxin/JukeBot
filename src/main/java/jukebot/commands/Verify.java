package jukebot.commands;

import com.patreon.resources.Pledge;
import jukebot.Database;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;
import jukebot.utils.Helpers;

@CommandProperties(description = "Receive your donor rewards if you're a patron")
public class Verify implements Command {

    public void execute(final Context context) {

        context.getChannel().sendTyping().queue();

        Helpers.getPatreonPledges(pledges -> {
            if (pledges == null) {
                context.sendEmbed("Pledge Verification", "An unknown error occurred during verification!");
                return;
            }

            final Pledge match = pledges.stream()
                    .filter(p -> p.getPatron().getSocialConnections().getDiscord() != null &&
                            Long.parseLong(p.getPatron().getSocialConnections().getDiscord().getUser_id()) == context.getAuthor().getIdLong())
                    .findFirst()
                    .orElse(null);

            if (match == null) {
                context.sendEmbed("Pledge Verification", "No Discord account linked to your Patreon account. Link your discord and try again.\n" +
                        "If you continue to receive this error, please [join here](https://discord.gg/xvtH2Yn)");
                return;
            }

            if (match.getDeclinedSince() != null) {
                context.sendEmbed("Pledge Verification", "It appears your payment has been declined. Please resolve this issue and then try again.\n" +
                        "If you believe this to be in error, please [join here](https://discord.gg/xvtH2Yn)");
                return;
            }

            final double pledgeAmount = (double) match.getAmountCents() / 100;
            int tier = 0;

            if (pledgeAmount >= 1 && pledgeAmount < 2) {
                tier = 1;
            } else if (pledgeAmount >= 2) {
                tier = 2;
            }

            context.sendEmbed("Pledge Verification", "Thanks for donating! **Your pledge: $" + String.format("%1$,.2f", pledgeAmount) + "**\n" +
                    "It looks like you qualify for **Tier " + tier + "**!\n\nYour reward has been applied. If for some reason you encounter issues, please join https://discord.gg/xvtH2Yn");

            Database.setTier(context.getAuthor().getIdLong(), tier);
        });
    }

}
