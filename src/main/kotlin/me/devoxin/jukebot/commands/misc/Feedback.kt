package me.devoxin.jukebot.commands.misc

import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.framework.Command
import me.devoxin.jukebot.framework.CommandProperties
import me.devoxin.jukebot.framework.Context
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.IncomingWebhookClient
import net.dv8tion.jda.api.entities.WebhookClient
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import java.time.Instant

@CommandProperties(aliases = ["suggest"], description = "Send feedback to the developer")
class Feedback : Command(ExecutionType.STANDARD) {
    private val webhookClient = createWebhook()

    private fun createWebhook(): IncomingWebhookClient? {
        return JukeBot.config.opt("feedback_webhook", null)?.takeIf { it.isBlank() }
            ?.let { WebhookClient.createClient(JukeBot.shardManager.shards.first(), it) }
    }

    override fun execute(context: Context) {
        val feedback = context.args.gatherNext("feedback").takeIf { it.isNotEmpty() }
            ?: return context.embed {
                setTitle("Missing Feedback")
                setDescription("You need to specify feedback for the developer.")
                setFooter("Misuse of this command will revoke your access.", null)
            }

        val embed = EmbedBuilder()
            .setColor(JukeBot.config.embedColour)
            .setTitle("New Feedback")
            .setDescription(feedback)
            .addField("Sender:", "${context.author.name}\n(${context.author.id})", true)
            .addField("Guild:", "${context.guild.name}\n(${context.guild.id})", true)
            .addBlankField(true)
            .setTimestamp(Instant.now())
            .build()

        webhookClient?.sendMessage(MessageCreateData.fromEmbeds(embed))?.queue()
        context.embed("Feedback sent!", "Thanks for your feedback! :)")
    }
}
