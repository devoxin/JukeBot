package jukebot.commands

import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context
//import net.dv8tion.jda.core.EmbedBuilder
//import net.dv8tion.jda.webhook.WebhookClient
//import net.dv8tion.jda.webhook.WebhookClientBuilder

@CommandProperties(description = "Send feedback to the developer")
class Feedback : Command(ExecutionType.STANDARD) {

    override fun execute(context: Context) {
    }

//    private val webhookClient: WebhookClient
//
//    init {
//        if (JukeBot.config.hasKey("feedback_webhook")) {
//            webhookClient = WebhookClientBuilder(JukeBot.config.getString("feedback_webhook")!!).build()
//        } else {
//            throw CommandInitializationError("feedback_webhook key is missing from config")
//        }
//    }
//
//    override fun execute(context: Context) {
//        if (context.argString.isEmpty()) {
//            return context.embed {
//                setTitle("Missing Feedback")
//                setDescription("You need to specify feedback for the developer.")
//                setFooter("Please note that misusing this command will see you blacklisted.", null)
//            }
//        }
//
//        val sender = "${context.author.name}#${context.author.discriminator}\n(${context.author.id})"
//        val guild = "${context.guild.name}\n(${context.guild.id})"
//
//        webhookClient.send(EmbedBuilder()
//                .setColor(JukeBot.config.embedColour)
//                .setTitle("New Feedback")
//                .setDescription(context.argString)
//                .addField("Sender:", sender, true)
//                .addField("Guild:", guild, true)
//                .addBlankField(true)
//                .setTimestamp(Instant.now())
//                .build()
//        )
//
//        context.embed("Feedback sent!", "Thanks for your feedback! :)")
//    }

    fun shutdown() {
//        webhookClient.close()
    }

}
