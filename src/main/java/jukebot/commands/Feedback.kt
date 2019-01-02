package jukebot.commands

import jukebot.JukeBot
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.webhook.WebhookClient
import net.dv8tion.jda.webhook.WebhookClientBuilder
import java.time.Instant

@CommandProperties(description = "Send feedback to the developer")
class Feedback : Command(ExecutionType.STANDARD) {

    private val webhookClient: WebhookClient = WebhookClientBuilder(JukeBot.config.getString("feedback_webhook")!!).build()

    override fun execute(context: Context) {
        if (context.argString.isEmpty()) {
            return context.embed {
                setTitle("Missing Feedback")
                setDescription("You need to specify feedback for the developer.")
                setFooter("Please note that misusing this command will see you blacklisted.", null)
            }
        }

        val sender = "${context.author.name}#${context.author.discriminator}\n(${context.author.id})"
        val guild = "${context.guild.name}\n(${context.guild.id})"

        webhookClient.send(EmbedBuilder()
                .setColor(JukeBot.embedColour)
                .setTitle("New Feedback")
                .setDescription(context.argString)
                .addField("Sender:", sender, true)
                .addField("Guild:", guild, true)
                .addBlankField(true)
                .setTimestamp(Instant.now())
                .build()
        )

        context.embed("Feedback sent!", "Thanks for your feedback! :)")
    }

    public fun shutdown() {
        webhookClient.close()
    }

}
