package me.devoxin.jukebot.commands.misc

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.WebhookClientBuilder
import club.minnced.discord.webhook.send.WebhookEmbed
import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.framework.Command
import me.devoxin.jukebot.framework.CommandProperties
import me.devoxin.jukebot.framework.Context
import java.time.OffsetDateTime

@CommandProperties(aliases = ["suggest"], description = "Send feedback to the developer")
class Feedback : Command(ExecutionType.STANDARD) {
    private val webhookClient: WebhookClient? = if ("feedback_webhook" in JukeBot.config) {
        WebhookClientBuilder(JukeBot.config["feedback_webhook"]).build()
    } else {
        null
    }

    override fun execute(context: Context) {
        val feedback = context.args.gatherNext("feedback").takeIf { it.isNotEmpty() }
            ?: return context.embed {
                setTitle("Missing Feedback")
                setDescription("You need to specify feedback for the developer.")
                setFooter("Misuse of this command will revoke your access.", null)
            }

        val sender = "${context.author.asTag}\n(${context.author.id})"
        val guild = "${context.guild.name}\n(${context.guild.id})"

        val fields = listOf(
            WebhookEmbed.EmbedField(true, "Sender:", sender),
            WebhookEmbed.EmbedField(true, "Guild:", guild),
            WebhookEmbed.EmbedField(true, "\u200b", "\u200b")
        )

        val whe = WebhookEmbed(
            OffsetDateTime.now(),
            JukeBot.config.embedColour.rgb,
            feedback,
            null,
            null,
            null,
            WebhookEmbed.EmbedTitle("New Feedback", null),
            null,
            fields
        )

        webhookClient?.send(whe)
        context.embed("Feedback sent!", "Thanks for your feedback! :)")
    }

    override fun destroy() {
        webhookClient?.close()
    }
}
