package jukebot.commands;

import com.patreon.resources.Pledge;
import jukebot.Database;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Helpers;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(description = "Receive your Patron benefits if you've donated")
public class Verify implements Command {

    public void execute(GuildMessageReceivedEvent e, String query) {
        e.getChannel().sendTyping().queue();
        Helpers.getPatreonPledges(pledges -> {
            if (pledges == null) {
                e.getChannel().sendMessage("An unknown error occurred while fetching campaign information").queue();
                return;
            }

            final Pledge match = pledges.stream()
                    .filter(p -> p.getPatron().getSocialConnections().getDiscord() != null &&
                            Long.parseLong(p.getPatron().getSocialConnections().getDiscord().getUser_id()) == e.getAuthor().getIdLong())
                    .findFirst()
                    .orElse(null);

            if (match == null) {
                e.getChannel().sendMessage("I couldn't find a Patreon account linked to your Discord account. Please link your Discord account and then try again\n" +
                        "If you continue to receive this error, please join https://discord.gg/xvtH2Yn").queue();
                return;
            }

            if (match.getDeclinedSince() != null) {
                e.getChannel().sendMessage("It appears your payment has been declined. Please resolve this issue and then verify.\n\n" +
                        "If you believe this to be in error, join the support server: https://discord.gg/xvtH2Yn").queue();
                return;
            }

            final double pledgeAmount = match.getAmountCents() / 100;
            int tier = 0;

            if (pledgeAmount > 0 && pledgeAmount < 2)
                tier = 1;
            else if (pledgeAmount > 0 && pledgeAmount >= 2)
                tier = 2;

            e.getChannel().sendMessage("Thanks for donating! **Your pledge: $" + pledgeAmount + "**\n" +
                    "It looks like you qualify for **Tier " + tier + "**!\n\n" +
                    "Your reward has been applied. If for some reason you encounter issues, please join https://discord.gg/xvtH2Yn").queue();

            Database.setTier(e.getAuthor().getIdLong(), tier);
        });
    }

}
